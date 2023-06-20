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
  (:refer-clojure :exclude [ref key])
  (:require
   [clojure.set :as set]
   [clojure.walk :as w]
   [darkleaf.di.destructuring-map :as map]
   [darkleaf.di.protocols :as p]
   [darkleaf.di.ref :as ref])
  (:import
   (clojure.lang IDeref IFn Var Indexed)
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

(declare find-or-build)

(defn- missing-dependency! [key]
  (throw (ex-info (str "Missing dependency " key)
                  {:type ::missing-dependency
                   :key  key})))

(defn- circular-dependency! [key]
  (throw (ex-info (str "Circular dependency " key)
                  {:type ::circular-dependency
                   :key  key})))

(defn- resolve-dep [{:as ctx, :keys [under-construction]} acc key dep-type]
  (if (under-construction key)
    (circular-dependency! key)
    (if-some [obj (find-or-build ctx key)]
      (assoc acc key obj)
      (if (= :optional dep-type)
        acc
        (missing-dependency! key)))))

(defn- resolve-deps [ctx deps]
  (reduce-kv (partial resolve-dep ctx)
             {}
             deps))

(defn- find-obj [{:keys [*built-map]} key]
  (get @*built-map key))

(defn- build-obj [{:as ctx, :keys [registry *current-key *built-map *stoppable-list]} key]
  (let [ctx           (update ctx :under-construction conj key)
        factory       (registry key)
        declared-deps (p/dependencies factory)
        resolved-deps (resolve-deps ctx declared-deps)
        stoppable     (p/build factory resolved-deps)
        obj           (p/unwrap stoppable)]
    (vswap! *stoppable-list conj stoppable)
    (vswap! *built-map  assoc key obj)
    obj))

(defn- find-or-build [ctx key]
  (?? (find-obj  ctx key)
      (build-obj ctx key)))

(defn- try-run [proc x]
  (try
    (proc x)
    nil
    (catch Throwable ex
      ex)))

(defn- try-run-all [proc coll]
  (->> coll
       (map #(try-run proc %))
       (filter some?)
       (seq)))

(defn- throw-many! [coll]
  (some->> coll
           seq
           (reduce combine-throwable)
           (throw)))

(defn- try-stop-objects [objects]
  (try-run-all p/stop objects))

(defn- try-stop-started [{:keys [*stoppable-list]}]
  (let [built-list @*stoppable-list]
    (vswap! *stoppable-list empty)
    (try-stop-objects built-list)))

(defn- try-build [ctx key]
  (try
    (?? (build-obj ctx key)
        (missing-dependency! key))
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
  See `instrument`, `update-key`.

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
  The container implements `AutoCloseable`, `Stoppable`, `IDeref`, `IFn` and `Indexed`.

  Use `with-open` in tests to stop the system reliably.

  You can pass a vector as the key argument to start many keys:

  ```clojure
  (with-open [root (di/start [`handler `helper])]
    (let [[handler helper] root]
       ...))
  ```

  See the tests for use cases.
  See `instrument`, `update-key`."
  [key & middlewares]
  (let [[key root-registry] (key->key&registry key)

        middlewares (concat [with-env with-ns root-registry] middlewares)
        registry    (apply-middleware nil-registry middlewares)
        ctx         {:*built-map         (volatile! {})
                     :*stoppable-list    (volatile! '())
                     :under-construction #{}
                     :registry           registry}
        obj         (try-build ctx key)]
    ^{:type   ::root
      ::print obj}
    (reify
      p/Stoppable
      (unwrap [_]
        obj)
      (stop [_]
        (->> (try-stop-started ctx)
             (throw-many!)))
      AutoCloseable
      (close [this]
        (p/stop this))
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
  [root]
  (p/stop root))

(defn ref
  "Returns a factory referencing to a key.

  ```clojure
  (def port (di/ref \"PORT\"))
  (defn server [{port `port}] ...)

  (def routes (di/template [[\"/posts\" (di/ref `handler)]]))

  (di/start `root {::my-abstraction (di/ref `my-implementation)})
  ```

  See `template`, `opt-ref`, `fmap`, `p/build`."
  [key]
  (ref/->Ref key :required))

(defn opt-ref
  "Returns a factory referencing to a possible undefined key.
  Produces nil in that case.

  See `template`, `ref`, `fmap`."
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
      (w/postwalk #(ref/build % deps) form))))

(defn fmap
  "Applies f to an object that the factory produces.
  f accepts a built object and returns updated one.

  f should not return a non-trivial instance of `p/Stoppable`.

  ```clojure
  (def port (-> (di/ref \"PORT\")
                (di/fmap parse-long)))
  ```

  See `ref`, `template`."
  [factory f & args]
  (reify p/Factory
    (dependencies [_]
      (p/dependencies factory))
    (build [_ deps]
      (let [original (p/build factory deps)
            obj (apply f (p/unwrap original) args)]
        (reify p/Stoppable
          (unwrap [_]
            obj)
          (stop [_]
            (p/stop original)))))))

(def ^:private key? (some-fn symbol? keyword? string?))

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

  See `start`, `update-key`, `fmap`."
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
                    (unwrap [_]
                      obj)
                    (stop [_]
                      (p/stop original))))))))))))

(defn update-key
  "A registry middleware for updating built objects.

  target is a key to update.
  f and args are keys.
  Also f can be a function in term of `ifn?`.
  f should not return a non-trivial instance of `p/Stoppable`.

  ```clojure
  (def routes [])
  (def subsystem-routes (di/template [[\"/posts\" (di/ref `handler)]]))

  (di/start ::root (di/update-key `routes conj `subsystem-routes))
  ```

  If you don't want to resolve keys like :some-name, you should use them in a in-place fn:

  ```clojure
  (di/update-key `key #(assoc %1 :some-name %2) `some-value)
  ```

  See `update`, `start`, `instrument`, `fmap`."
  [target f & args]
  {:pre [(or (key? f)
             (ifn? f))
         (every? key? args)]}
  (let [own-keys (cond-> (set args)
                   (key? f) (conj f))]
    (fn [registry]
      (fn [key]
        (let [factory (registry key)]
          (if (not= target key)
            factory
            (reify p/Factory
              (dependencies [_]
                (merge (p/dependencies factory)
                       (zipmap own-keys (repeat :required))))
              (build [_ deps]
                (let [f        (deps f f)
                      args     (map deps args)
                      original (p/build factory deps)
                      obj      (apply f (p/unwrap original) args)]
                  (reify p/Stoppable
                    (unwrap [_]
                      obj)
                    (stop [_]
                      (p/stop original))))))))))))

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
  (let [*added? (volatile! false)]
    (fn [registry]
      (fn [key]
        (let [factory (registry key)]
          (if @*added?
            factory
            (do
              (vreset! *added? true)
              (reify p/Factory
                (dependencies [_]
                  (-> factory
                      p/dependencies
                      (assoc dep-key :required)))
                (build [_ deps]
                  (p/build factory deps))))))))))

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

(defn- wrap-stop [obj variable]
  (if-some [stop (-> variable meta ::stop)]
    (reify p/Stoppable
      (unwrap [_] obj)
      (stop [_] (stop obj)))
    obj))

(defn- build-fn [variable deps]
  (let [max-arity (->> variable
                       arglists
                       (map count)
                       (reduce max 0) long)]
    (case max-arity
      0 (-> (variable)
            (wrap-stop variable))
      1 (-> (variable deps)
            (wrap-stop variable))
      (partial variable deps))))

(defn- var->factory-defn [variable]
  (when (defn? variable)
    (reify p/Factory
      (dependencies [_]
        (dependencies-fn variable))
      (build [_ deps]
        (build-fn variable deps)))))

(defn- var->factory-meta-deps [variable]
  (if-some [deps (some-> variable meta ::deps (zipmap (repeat :required)))]
    (reify p/Factory
      (dependencies [_]
        deps)
      (build [_ deps]
        (partial variable deps)))))

(defn- var->factory-default [variable]
  (reify p/Factory
    (dependencies [_]
      (p/dependencies @variable))
    (build [_ deps]
      (p/build @variable deps))))

(defn- var->factory [variable]
  (?? (var->factory-meta-deps variable)
      (var->factory-defn variable)
      (var->factory-default variable)))

(extend-protocol p/Factory
  nil
  (dependencies [_] nil)
  (build [_ _] nil)

  Object
  (dependencies [_] nil)
  (build [this _] this))

(extend-protocol p/Stoppable
  nil
  (unwrap [_] nil)
  (stop [_])
  Object
  (unwrap [this] this)
  (stop [_]))

(derive ::root     ::reified)
(derive ::template ::reified)

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
      (if-some [parser (some-> key try-namespace keyword cmap)]
        (-> key name opt-ref (fmap #(some-> % parser)))
        (registry key)))))

(defn rename-deps [target rmap]
  (let [inverted-rmap (set/map-invert rmap)]
    (fn [registry]
      (fn [key]
        (let [factory (registry key)]
          (if (= target key)
            (reify p/Factory
              (dependencies [_]
                (let [deps (p/dependencies factory)]
                  (update-keys deps rmap)))
              (build [this deps]
                (let [deps (update-keys deps inverted-rmap)]
                  (p/build factory deps))))
            factory))))))
