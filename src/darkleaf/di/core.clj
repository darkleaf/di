;; *********************************************************************
;; * Copyright (c) 2022 Mikhail Kuzmin
;; *
;; * This program and the accompanying materials are made
;; * available under the terms of the Eclipse Public License 2.0
;; * which is available at https://www.eclipse.org/legal/epl-2.0/
;; *
;; * SPDX-License-Identifier: EPL-2.0
;; **********************************************************************/

(ns darkleaf.di.core
  (:refer-clojure :exclude [ref key ns-publics derive])
  (:require
   [clojure.core :as c]
   [clojure.set :as set]
   [clojure.walk :as w]
   [darkleaf.di.destructuring-map :as map]
   [darkleaf.di.protocols :as p]
   [darkleaf.di.ref :as ref])
  (:import
   (clojure.lang IDeref IFn Var Indexed ILookup)
   (java.io FileNotFoundException Writer)
   (java.lang AutoCloseable)))

(set! *warn-on-reflection* true)

(defmacro ^:private ??
  ([] nil)
  ([x] x)
  ([x & next]
   `(if-some [x# ~x]
      x#
      (?? ~@next))))

(def ^:private dependency-type-priority
  {:required 1
   :optional 2})

(defn combine-dependencies
  "Combines dependencies. Use it with `reduce`.
  Dependencies are a hash map of a key and a dependency type."
  ([]
   {})
  ([a b]
   (merge-with (fn [x y]
                 (min-key dependency-type-priority x y))
               a b)))

(defn- combine-throwable
  "Combines throwables. Use it with `reduce`."
  ([] nil)
  ([^Throwable a b]
   (.addSuppressed a b)
   a))

(defn- try-run [proc]
  (try
    (proc)
    nil
    (catch Throwable ex
      ex)))

(defn- try-run-all [procs]
  (->> procs
       (map try-run)
       (filter some?)
       (seq)))

(defn- throw-many! [coll]
  (some->> coll
           seq
           (reduce combine-throwable)
           (throw)))

(defn- try-stop-started [{:keys [*stop-list]}]
  (let [stops @*stop-list]
    (vswap! *stop-list empty)
    (try-run-all stops)))

(defn- missing-dependency! [{:keys [k path]}]
  (throw (ex-info (str "Missing dependency " k)
                  {:type ::missing-dependency
                   :path path
                   :key  k})))

(defn- circular-dependency! [{:keys [k path]}]
  (throw (ex-info (str "Circular dependency " k)
                  {:type ::circular-dependency
                   :path path
                   :key  k})))

(defn- maximum-recursion-depth! [{:keys [build-iteration]} {:keys [k path]}]
  (throw (ex-info "Exceeded maximum build iterations"
                  {:iterations build-iteration
                   :path       (take-last 20 path)
                   :key        k})))

(defn- find-obj [{:keys [built-map]} key]
  (get built-map key))

(defn- obj-built? [{:keys [built-map]} key]
  (contains? built-map key))

(defn- deps [{:as ctx :keys [registry]} {parent-k :k}]
  (let [{built true, to-build false}
        (->> (registry parent-k)
             p/dependencies
             (group-by (fn [[dep-k _]] (obj-built? ctx dep-k))))]
    {:deps-to-build to-build
     :built-deps    (into {}
                          (keep (fn [[dep-k _dep-type]]
                                  (when-some [dep-obj (find-obj ctx dep-k)]
                                    [dep-k dep-obj])))
                          built)}))

(defn- build-obj [{:as ctx :keys [registry *stop-list]} {:as cur :keys [k k-type]} deps]
  (let [factory (registry k)
        obj     (p/build factory deps)]
    (vswap! *stop-list conj #(p/demolish factory obj))
    (when (and (nil? obj) (= :required k-type))
      (missing-dependency! cur))
    (assoc-in ctx [:built-map k] obj)))

(defn- push-to-build-stack [stack path & deps]
  (into stack
        (map (fn [[dep dep-type]]
               {:k dep :k-type dep-type :path path}))
        (reverse deps)))

(defn- build-obj&deps [ctx key-to-build]
  (loop [stack (push-to-build-stack () [] [key-to-build :required])
         ctx   ctx]
    (let [{:as cur :keys [k path]}                   (peek stack)
          {:as ctx :keys [deferred build-iteration]} (update ctx :build-iteration inc)
          {:keys [deps-to-build built-deps]}         (some->> cur (deps ctx))
          ready-to-build?                            (empty? deps-to-build)]
      (cond
        (nil? cur)
        ctx

        (> build-iteration 1000000)
        (maximum-recursion-depth! ctx cur)

        (obj-built? ctx k)
        (recur (pop stack) ctx)

        ready-to-build?
        (recur (pop stack) (build-obj ctx cur built-deps))

        (deferred k)
        (circular-dependency! cur)

        :else
        (recur (apply push-to-build-stack stack (conj path k) deps-to-build)
               (update ctx :deferred conj k))))))

(defn- try-build [ctx key]
  (try
    (-> ctx
        (build-obj&deps key)
        (find-obj key))
    (catch Throwable ex
      (let [exs (try-stop-started ctx)
            exs (cons ex exs)]
        (throw-many! exs)))))

(defn- nil-registry [key]
  nil)

(defn- apply-middleware [registry middleware]
  (cond
    (fn? middleware)      (middleware registry)
    (map? middleware)     (fn [key]
                            (?? (get middleware key)
                                (registry key)))
    (seqable? middleware) (reduce apply-middleware
                                  registry middleware)
    :else                 (throw (IllegalArgumentException. "Wrong middleware kind"))))

(declare var->factory)

(defn- try-requiring-resolve [key]
  (when (qualified-symbol? key)
    (try
      (requiring-resolve key)
      (catch FileNotFoundException _ nil))))

(defn- with-ns
  "Adds support to the registry for looking up vars."
  [registry]
  (fn [key]
    (?? (some-> key
                try-requiring-resolve
                var->factory)
        (registry key))))

(defn- with-env
  "Adds support to the registry for looking up environment variables."
  [registry]
  (fn [key]
    (?? (when (string? key)
          (System/getenv key))
        (registry key))))

(declare ref template)

(defn- key->key&registry [key]
  (cond
    (vector? key) [::implicit-root {::implicit-root (->> key (map ref) template)}]
    (map? key)    [::implicit-root {::implicit-root (-> key (update-vals ref) template)}]
    true          [key nil]))

(defn ^AutoCloseable start
  "Starts a system of dependent objects.

  key is a name of the system root.
  Use symbols for var names, keywords for abstract dependencies,
  and strings for environments variables.

  key is looked up in a registry.
  By default registry uses Clojure namespaces and system env
  to resolve symbols and strings, respectively.

  You can extend it with registry middlewares.
  Each middleware can be one of the following form:

  - a function `registry -> key -> Factory`
  - a map of key and `p/Factory` instance
  - nil, as no-op middleware
  - a sequence of the previous forms

  Middlewares also allows you to instrument built objects.
  It's useful for logging, schema validation, AOP, etc.
  See `update-key`.

  ```clojure
  (di/start `root
            {:my-abstraction implemntation
             `some-key replacement
             \"LOG_LEVEL\" \"info\"}
            [dev-middlwares test-middlewares]
            (if dev-routes?
              (di/update-key `route-data conj `dev-route-data)
            (di/instrument `log))
  ```

  Returns a container contains started root of the system.
  The container implements `AutoCloseable`, `IDeref`, `IFn`, `Indexed` and `ILookup`.

  Use `with-open` in tests to stop the system reliably.

  You can pass a vector as the key argument to start many keys:

  ```clojure
  (with-open [root (di/start [`handler `helper])]
    (let [[handler helper] root]
       ...))
  ```

  See the tests for use cases.
  See `update-key`."
  [key & middlewares]
  (let [[key root-registry] (key->key&registry key)

        middlewares (concat [with-env with-ns root-registry] middlewares)
        registry    (apply-middleware nil-registry middlewares)
        ctx         {:*stop-list      (volatile! '())
                     :registry        registry
                     :built-map       {}
                     :deferred        #{}
                     :build-iteration 0}
        obj         (try-build ctx key)]
    ^{:type   ::root
      ::print obj}
    (reify
      AutoCloseable
      (close [_]
        (->> (try-stop-started ctx)
             (throw-many!)))
      IDeref
      (deref [_]
        obj)
      Indexed
      (nth [_    i]
        (nth obj i))
      (nth [_    i not-found]
        (nth obj i not-found))
      (count [_]
        (count obj))
      ILookup
      (valAt [_  key]
        (get obj key))
      (valAt [_  key not-found]
        (get obj key not-found))
      IFn
      (call [_]
        (.call ^IFn obj))
      (run [_]
        (.run ^IFn obj))
      (invoke [this]
        (.invoke ^IFn obj))
      (invoke [_          a1]
        (.invoke ^IFn obj a1))
      (invoke [_          a1 a2]
        (.invoke ^IFn obj a1 a2))
      (invoke [_          a1 a2 a3]
        (.invoke ^IFn obj a1 a2 a3))
      (invoke [_          a1 a2 a3 a4]
        (.invoke ^IFn obj a1 a2 a3 a4))
      (invoke [_          a1 a2 a3 a4 a5]
        (.invoke ^IFn obj a1 a2 a3 a4 a5))
      (invoke [_          a1 a2 a3 a4 a5 a6]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20))
      (invoke [_          a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 args]
        (.invoke ^IFn obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 args))
      (applyTo [_ args]
        (.applyTo ^IFn obj args)))))

(defn stop
  "Stops the root of a system"
  [^AutoCloseable root]
  (.close root))


(def ^:private key? (some-fn symbol? keyword? string?))

(defn ref
  "Returns a factory referencing to a key.

  ```clojure
  (def port (di/ref \"PORT\"))
  (defn server [{port `port}] ...)

  (def routes (di/template [[\"/posts\" (di/ref `handler)]]))

  (di/start `root {::my-abstraction (di/ref `my-implementation)})
  ```

  See `template`, `opt-ref`, `derive`, `p/build`."
  [key]
  (ref/->Ref key :required))

(defn opt-ref
  "Returns a factory referencing to a possible undefined key.
  Produces nil in that case.

  See `template`, `ref`, `derive`."
  [key]
  (ref/->Ref key :optional))

(defn template
  "Returns a factory for templating a data-structure.
  Replaces `ref` or `opt-ref` instances with built objects.

  ```clojure
  (def routes (di/template [[\"/posts\" (di/ref `handler)]]))
  ```

  See `ref` and `opt-ref`."
  [form]
  ^{:type   ::template
    ::print form}
  (reify p/Factory
    (dependencies [_]
      (->> form
           (tree-seq coll? seq)
           (map ref/deps)
           (reduce combine-dependencies)))
    (build [_ deps]
      (w/postwalk #(ref/build % deps) form))
    (demolish [_ _])))

(defn derive
  "Applies `f` to an object built from `key`.

  ```clojure
  (def port (-> (di/derive \"PORT\" (fnil parse-long \"8080\"))))
  ```

  See `ref`, `template`."
  [key f & args]
  {:pre [(key? key)
         (ifn? f)]}
  (reify p/Factory
    (dependencies [_]
      {key :optional})
    (build [_ deps]
      (apply f (deps key) args))
    (demolish [_ _])))

;; We currently don't need this middleware.
;; It should be rewritten as `update-key`.
;; Also it has a poor documentation and a test coverage.
;; We might add a new implementation later.
#_
(defn instrument
  "A registry middleware for instrumenting or decorating built objects.
  Use it for logging, schema checking, AOP, etc.

  f and args are keys.
  Also f can be a function in term of `ifn?`.

  A resolved f must be a function of `[object key & args] -> new-object`.
  f should not return a non-trivial instance of `p/Stoppable`.

  It is smart enough not to instrument f's dependencies with the same f
  to avoid circular dependencies.

  ```clojure
  (defn stateful-instrumentaion [{state :some/state} key object arg1 arg2] ...)
  (di/start ::root (di/instrument `stateful-instrumentation `arg1 ::arg2 \"arg3\")))

  (defn stateless-instrumentaion [key object arg1 arg2 arg3] ...)
  (di/start ::root (di/instrument   stateless-instrumentation `arg1 ::arg2 \"arg3\"))
  (di/start ::root (di/instrument #'stateless-instrumentation `arg1 ::arg2 \"arg3\"))
  ```

  See `start`, `update-key`."
  [f & args]
  {:pre [(or (key? f)
             (ifn? f))
         (every? key? args)]}
  (let [own-keys            (cond-> (set args)
                              (key? f) (conj f))
        *under-construction (volatile! #{})]
    (fn [registry]
      (fn [key]
        (vswap! *under-construction conj key)
        (let [factory (registry key)]
          (if (some @*under-construction own-keys)
            (reify p/Factory
              (dependencies [_]
                (p/dependencies factory))
              (build [_ deps]
                (vswap! *under-construction disj key)
                (p/build factory deps)))
            (reify p/Factory
              (dependencies [_]
                (merge (p/dependencies factory)
                       (zipmap own-keys (repeat :required))))
              (build [_ deps]
                (vswap! *under-construction disj key)
                (let [f        (deps f f)
                      args     (map deps args)
                      original (p/build factory deps)
                      obj      (apply f key (p/unwrap original) args)]
                  (reify p/Stoppable
                    ;; ???
                    ;; (unwrap [_]
                    ;;   (p/unwrap obj))
                    ;; (stop [_]
                    ;;   (p/stop original)
                    ;;   (p/stop obj))
                    (unwrap [_]
                      obj)
                    (stop [_]
                      (p/stop original))))))))))))

(defn update-key
  "A registry middleware for updating built objects.

  target is a key to update.
  f and args are intances of `p/Factory`.
  For example, a factory can be a regular object or `(di/ref key)`.

  ```clojure
  (def routes [])
  (def subsystem-routes (di/template [[\"/posts\" (di/ref `handler)]]))

  (di/start ::root (di/update-key `routes conj (di/ref `subsystem-routes)))
  ```

  See `start`, `derive`."
  [target f & args]
  {:pre [(key? target)]}
  (let [new-key      (gensym (str (symbol target) "+di-update-key#"))
        f-key        (gensym "darkleaf.di.core/update-key-f#")
        arg-keys     (for [_ args] (gensym "darkleaf.di.core/update-key-arg#"))
        new-factory  (reify p/Factory
                       (dependencies [_]
                         (zipmap (concat [new-key f-key] arg-keys)
                                 (repeat :optional)))
                       (build [_ deps]
                         (let [t    (deps new-key)
                               f    (deps f-key)
                               args (map deps arg-keys)]
                           (apply f t args)))
                       (demolish [_ _]))
        own-registry (zipmap (cons f-key arg-keys)
                             (cons f     args))]
    (fn [registry]
      (fn [key]
        (cond
          (= new-key key)
          (registry target)

          (= target key)
          new-factory

          :else
          (?? (own-registry key)
              (registry key)))))))

(defn add-side-dependency
  "A registry middleware for adding side dependencies.
  Use it for migrations or other side effects.


  ```clojure
  (defn flyway [{url \"DATABASE_URL\"}]
    (.. (Flyway/configure)
        ...))

  (di/start ::root (di/add-side-dependency `flyway))
  ```"
  [dep-key]
  (let [*orig-key     (volatile! nil)
        *orig-factory (volatile! nil)
        new-key       (gensym "darkleaf.di.core/new-key#")
        new-factory   (reify p/Factory
                        (dependencies [_]
                          ;; array-map preserves order of keys
                          {new-key :required
                           dep-key :required})
                        (build [_ deps]
                          (new-key deps))
                        (demolish [_ _]))]
    (fn [registry]
      (fn [key]
        (when (nil? @*orig-key)
          (vreset! *orig-key key))
        (when (nil? @*orig-factory)
          (vreset! *orig-factory (registry key)))
        (cond
          (= @*orig-key key) new-factory
          (= new-key key)    @*orig-factory
          :else              (registry key))))))

(defn- arglists [variable]
  (-> variable meta :arglists))

(defn- defn? [variable]
  (-> variable arglists seq boolean))

(defn- dependencies-fn [variable]
  (->> variable
       arglists
       (map first)
       (filter map?)
       (map map/dependencies)
       (reduce combine-dependencies)))

(defn- stop-fn [variable]
  (-> variable meta (::stop (fn no-op [_]))))

(defn- var->0-component [variable]
  (let [stop (stop-fn variable)]
    (reify p/Factory
      (dependencies [_])
      (build [_ _]
        (variable))
      (demolish [_ obj]
        (stop obj)))))

(defn- var->1-component [variable]
  (let [deps (dependencies-fn variable)
        stop (stop-fn variable)]
    (reify p/Factory
      (dependencies [_]
        deps)
      (build [_ deps]
        (variable deps))
      (demolish [_ obj]
        (stop obj)))))

(defn- var->0-service [variable]
  variable)

(defn- var->service [variable]
  (let [deps (dependencies-fn variable)]
    (reify p/Factory
      (dependencies [_]
         deps)
      (build [_ deps]
         (partial variable deps))
      (demolish [_ _]))))

(defn- var->factory-defn [variable]
  (when (defn? variable)
    (let [m         (meta variable)
          has-stop? (contains? m ::stop)
          kind      (::kind m (cond has-stop? :component))
          arities   (->> variable arglists (map count))]
      (case kind
        :component (condp = arities
                     [0] (var->0-component variable)
                     [1] (var->1-component variable)
                     (throw (ex-info
                             "The component must only have 0 or 1 arity"
                             {:variable variable
                              :arities  arities})))
        #_service  (condp = arities
                     [0] (var->0-service variable)
                     (var->service variable))))))

(defn- var->factory-meta-deps
  "for multimethods"
  [variable]
  (if-some [deps (some-> variable meta ::deps (zipmap (repeat :required)))]
    (reify p/Factory
      (dependencies [_]
        deps)
      (build [_ deps]
        (partial variable deps))
      (demolish [_ _]))))

(defn- var->factory-default [variable]
  @variable)

(defn- var->factory [variable]
  (?? (var->factory-meta-deps variable)
      (var->factory-defn variable)
      (var->factory-default variable)))

(extend-protocol p/Factory
  nil
  (dependencies [_] nil)
  (build [_ _] nil)
  (demolish [_ _] nil)

  Object
  (dependencies [_] nil)
  (build [this _] this)
  (demolish [_ _] nil))

(c/derive ::root     ::reified)
(c/derive ::template ::reified)

(defmethod print-method ::reified [o ^Writer w]
  (.write w "#")
  (.write w (-> o type symbol str))
  (.write w " ")
  (binding [*out* w]
    (pr (-> o meta ::print))))

(defn- try-namespace [x]
  (when (ident? x)
    (namespace x)))

(defn env-parsing
  "A registry middleware for parsing environment variables.
  You can define a dependency of env as a string key like \"PORT\",
  and its value will be a string.
  With this middleware, you can define it as a qualified keyword like :env.long/PORT,
  and its value will be a number.
  cmap is a map of prefixes and parsers.

  ```clojure
  (defn root [{port :env.long/PORT}]
    ...)

  (di/start `root (di/env-parsing :env.long parse-long
                                  :env.edn  edn/read-string
                                  :env.json json/read-value))
  ```"
  {:added "2.3.0"}
  [& {:as cmap}]
  {:pre [(map? cmap)
         (every? simple-keyword? (keys cmap))
         (every? ifn? (vals cmap))]}
  (fn [registry]
    (fn [key]
      (let [key-ns   (some-> key try-namespace keyword)
            key-name (name key)
            parser   (cmap key-ns)]
        (if (some? parser)
           (reify p/Factory
             (dependencies [_]
               {key-name :optional})
             (build [_ deps]
               (some-> key-name deps parser))
             (demolish [_ _]))
           (registry key))))))

;; (defn rename-deps [target rmap]
;;   (let [inverted-rmap (set/map-invert rmap)]
;;     (fn [registry]
;;       (fn [key]
;;         (let [factory (registry key)]
;;           (if (= target key)
;;             (reify p/Factory
;;               (dependencies [_]
;;                 (let [deps (p/dependencies factory)]
;;                   (set/rename-keys deps rmap)))
;;               (build [this deps]
;;                 (let [deps (set/rename-keys deps inverted-rmap)]
;;                   (p/build factory deps))))
;;             factory))))))


(defn- usefull-var? [var]
  (and (bound? var)
       (some? @var)))

(defn ns-publics
  "A registry middleware that interprets a whole namespace as a component.
  A component will be a map of var names to corresponding components.

  The key of a component is a keyword with the namespace `:ns-publics`
  and a name containing the name of a target ns.
  For example `:ns-publics/io.gihub.my.ns`.

  This enables access to all public components, which is useful for testing.

  See the test darkleaf.di.tutorial.x-ns-publics-test.

  ```clojure
  (di/start :ns-publics/io.gihub.my.ns (di/ns-publics))
  ```"
  []
  (fn [registry]
    (fn [key]
      (if (and (qualified-keyword? key)
               (= "ns-publics" (namespace key)))
        (let [component-ns      (symbol (name key))
              component-vars    (do
                                  (require component-ns)
                                  (c/ns-publics component-ns))
              component-symbols (->> component-vars
                                     vals
                                     (filter usefull-var?)
                                     (map symbol))
              deps              (zipmap component-symbols
                                        (repeat :required))]
          (reify p/Factory
            (dependencies [_this]
              deps)
            (build [_this deps]
              (update-keys deps #(-> % name keyword)))
            (demolish [_ _])))
        (registry key)))))
