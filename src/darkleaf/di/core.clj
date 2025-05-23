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
  (:refer-clojure :exclude [ref key ns-publics derive with-open])
  (:require
   [clojure.core :as c]
   [clojure.set :as set]
   [clojure.walk :as w]
   [darkleaf.di.destructuring-map :as map]
   [darkleaf.di.protocols :as p]
   [darkleaf.di.ref :as ref]
   [darkleaf.di.utils :as u :refer [?? try*]])
  (:import
   (clojure.lang IDeref IFn Var Indexed ILookup)
   (java.io FileNotFoundException Writer)
   (java.lang AutoCloseable)
   (java.util.function Function)
   (java.util.concurrent ConcurrentHashMap)))

(set! *warn-on-reflection* true)

(def ^:private dependency-type-priority
  {:required 1
   :optional 2})

(defn combine-dependencies
  "Combines dependencies. Use it with `reduce`.
  Dependencies are a hash map of a key and a dependency type."
  ([] {})
  ([a] a)
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

(def ^:private remove-implementation-details
  (remove #(-> % :factory p/description ::implementation-detail)))

(defn- missing-dependency! [stack]
  (let [key (-> stack peek :key)]
    (throw (ex-info (str "Missing dependency " key)
                    {:type  ::missing-dependency
                     :stack (into []
                                  (comp
                                   remove-implementation-details
                                   (map :key))
                                  stack)}))))

(defn- circular-dependency! [stack]
  (let [key (-> stack peek :key)]
    (throw (ex-info (str "Circular dependency " key)
                    {:type  ::circular-dependency
                     :stack (into []
                                  (comp
                                   remove-implementation-details
                                   (map :key))
                                  stack)}))))

(defn- update-head [stack f & args]
 (let [head (peek stack)
       tail (pop stack)]
   (conj tail (apply f head args))))

(defn- stack-frame [key dep-type factory]
  (let [deps (p/dependencies factory)]
    {:key            key
     :dep-type       dep-type
     :factory        factory
     :declared-deps  deps
     :remaining-deps (seq deps)}))

(defn- ->add-stop [{:keys [*stop-list]}]
  (fn [stop]
    (swap! *stop-list conj stop)))

(defn- build-obj* [ctx built-map head]
  (let [factory       (:factory head)
        declared-deps (:declared-deps head)
        built-deps    (select-keys built-map (keys declared-deps))]
    (p/build factory built-deps (->add-stop ctx))))

(defn- build-obj [ctx built-map stack]
  (try
    (build-obj* ctx built-map (peek stack))
    (catch Exception ex
      (throw (ex-info "A failure occurred during the build process"
                      {:type  ::build-failure
                       :stack (into []
                                    (comp
                                     remove-implementation-details
                                     (map :key))
                                    stack)}
                      ex)))))

(defn- build [{:keys [registry] :as ctx} key]
  (loop [stack     (list (stack-frame key :required (registry key)))
         built-map {}]
    (if (empty? stack)
      (built-map key)

      (let [head           (peek stack)
            tail           (pop stack)
            key            (:key head)
            dep-type       (:dep-type head)
            factory        (:factory head)
            remaining-deps (:remaining-deps head)]

        (cond
          (contains? built-map key)
          (recur tail built-map)

          (u/seq-contains? (map :key tail) key)
          (circular-dependency! stack)

          (seq remaining-deps)
          (let [[key dep-type] (first remaining-deps)]
            (recur (-> stack
                       (update-head update :remaining-deps rest)
                       (conj (stack-frame key dep-type (registry key))))
                   built-map))

          :else
          (let [obj (build-obj ctx built-map stack)]
            (case [obj dep-type]
              [nil :optional] (recur tail built-map)
              [nil :required] (missing-dependency! stack)
              (recur tail (assoc built-map key obj)))))))))

(defn- try-run [proc]
  (try*
    (proc)
    nil
    (catch* [Exception AssertionError] ex
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
  (let [[stops _] (swap-vals! *stop-list empty)]
    (try-run-all stops)))

(defn- try-build [ctx key]
  (try*
    (build ctx key)
    (catch* [Exception AssertionError] ex
      (let [exs (try-stop-started ctx)
            exs (cons ex exs)]
        (throw-many! exs)))))

(def ^:private undefined-factory
  (reify p/Factory
    (dependencies [_])
    (build [_ _ _] nil)
    (description [_]
      {::kind :undefined})))

(defn- undefined-registry [key]
  undefined-factory)

(defn- apply-map [registry map]
  (fn [key]
    (if-some [[_ trivial] (find map key)]
      trivial
      (registry key))))

(defn- apply-middlewares [registry middlewares]
  (loop [registry                    registry
         [mw & tail :as middlewares] middlewares]
    (cond
      (empty? middlewares)    registry
      (nil? mw)               (recur registry tail)
      (fn? mw)                (recur (mw registry) tail)
      (map? mw)               (recur (apply-map registry mw) tail)
      (instance? Function mw) (recur (.apply ^Function mw registry) tail)
      (sequential? mw)        (recur registry (concat mw tail))
      :else                   (throw (IllegalArgumentException. "Wrong middleware kind")))))

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
          (reify p/Factory
            (dependencies [_])
            (build [_ _ _]
              (System/getenv key))
            (description [_]
              {::kind :env})))
        (registry key))))

(declare ref template update-description)

(defn- implicit-root [key]
  (let [[factory
         root-keys] (cond
                      (vector? key) [(->> key (map ref) template)
                                     (set key)]
                      (map? key)    [(->  key (update-vals ref) template)
                                     (set (vals key))]
                      :else         [(->  key ref)
                                     #{key}])
        factory (reify p/Factory
                  (dependencies [_]
                    (concat (p/dependencies factory)
                            {::side-dependency :required}))
                  (build [_ deps add-stop]
                    (p/build factory deps add-stop))
                  (description [_]
                    {::implementation-detail true}))]
    (fn [registry]
      (fn [key]
        (cond
          (= ::implicit-root key)   factory
          (contains? root-keys key) (-> (registry key)
                                        (update-description assoc ::root true))
          :else                     (registry key))))))

(defn- with-internals [registry]
  (fn [key]
    (case key
      ::side-dependency (reify p/Factory
                          (dependencies [_])
                          (build [_ _ _] '_)
                          (description [_]
                            {::implementation-detail true}))
      (registry key))))

(def ^:private initial-registry
  (-> undefined-registry with-env with-ns with-internals))

(defn- start* ^AutoCloseable [key middlewares]
  (let [registry (apply-middlewares initial-registry middlewares)
        ctx      {:registry   registry
                  :*stop-list (atom '())}
        obj      (try-build ctx key)]
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


(defn start
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
  ^AutoCloseable [key & middlewares]
  (start* ::implicit-root [middlewares (implicit-root key)]))

(defn stop
  "Stops the root of a system"
  [^AutoCloseable root]
  (.close root))


(defn- update-description [factory f & args]
  (reify p/Factory
    (dependencies [_]
      (p/dependencies factory))
    (build [_ deps add-stop]
      (p/build factory deps add-stop))
    (description [_]
      (apply f (p/description factory) args))))

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
      (transduce (map p/dependencies)
                 combine-dependencies
                 (tree-seq coll? seq form)))
    (build [_ deps add-stop]
      (w/postwalk #(p/build % deps add-stop) form))
    (description [_]
      {::kind    :template
       :template form})))

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
    (build [_ deps _]
      (apply f (deps key) args))
    (description [_]
      {::kind :derive
       :key   key
       :f     f
       :args  args})))

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
  (fn [registry]
    (let [factory (registry target)
          _       (when (= undefined-factory factory)
                    (throw (ex-info (str "Can't update non-existent key " target)
                                    {:type ::non-existent-key
                                     :key  target})))
          factory (reify p/Factory
                    (dependencies [_]
                      (transduce (map p/dependencies)
                                 combine-dependencies
                                 (cons factory (cons f args))))
                    (build [_ deps add-stop]
                      (let [t    (p/build factory deps add-stop)
                            f    (p/build f       deps add-stop)
                            args (for [arg args] (p/build arg deps add-stop))]
                        (apply f t args)))
                    (description [_]
                      (-> (p/description factory)
                          (update ::update-key u/conjv
                                  (map p/description (cons f args))))))]
      (fn [key]
        (if (= target key)
          factory
          (registry key))))))

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
  (fn [registry]
    (fn [key]
      (let [factory (registry key)]
        (condp = key
          ::side-dependency (reify p/Factory
                              (dependencies [_]
                                ;; This is an incorrect implementation that does not preserve order.
                                ;; (assoc (p/dependencies factory)
                                ;;        dep-key :required)
                                (concat (p/dependencies factory)
                                        {dep-key :required}))
                              (build [_ deps add-stop]
                                (p/build factory deps add-stop))
                              (description [_]
                                (p/description factory)))
          dep-key           (update-description factory assoc ::side-dependency true)
          factory)))))


(defn- arglists [variable]
  (-> variable meta :arglists))

(defn- defn? [variable]
  (-> variable arglists seq boolean))

(defn- dependencies-fn [variable]
  (transduce (comp
              (map first)
              (filter map?)
              (map map/dependencies))
             combine-dependencies
             (arglists variable)))

(defn- stop-fn [variable]
  (-> variable meta (::stop (fn no-op [_]))))

(defn- validate-obj! [obj variable]
  (when (nil? obj)
    (throw (ex-info "A component fn must not return nil"
                    {:type     ::nil-return
                     :variable variable}))))

(defn- var->0-component [variable]
  (let [stop (stop-fn variable)]
    (reify p/Factory
      (dependencies [_])
      (build [_ _ add-stop]
        (let [obj (variable)]
          (validate-obj! obj variable)
          (add-stop #(stop obj))
          obj))
      (description [_]
        {::kind :component}))))

(defn- var->1-component [variable]
  (let [deps (dependencies-fn variable)
        stop (stop-fn variable)]
    (reify p/Factory
      (dependencies [_]
        deps)
      (build [_ deps add-stop]
        (let [obj (variable deps)]
          (validate-obj! obj variable)
          (add-stop #(stop obj))
          obj))
      (description [_]
        {::kind :component}))))

(defn- service-factory [variable declared-deps]
  (reify p/Factory
    (dependencies [_]
      declared-deps)
    (build [_ deps _]
      (-> variable
          (partial deps)
          (with-meta {:type   ::service
                      ::print variable})))
    (description [_]
      {::kind :service})))

(defn- var->0-service [variable]
  (reify p/Factory
    (dependencies [_])
    (build [_ _ _]
      variable)
    (description [_]
      {::kind :service})))

(defn- var->service [variable]
  (let [deps (dependencies-fn variable)]
    (service-factory variable deps)))

(defn- var->factory-defn [variable]
  (when (defn? variable)
    (let [m         (meta variable)
          has-stop? (contains? m ::stop)
          kind      (::kind m (cond has-stop? :component))
          arities   (->> variable arglists (map count))]
      (case kind
        :component (case arities
                     [0] (var->0-component variable)
                     [1] (var->1-component variable)
                     (throw (ex-info
                             "A component fn must only have 0 or 1 arity"
                             {:type     ::invalid-arity
                              :variable variable
                              :arities  arities})))
        #_service  (case arities
                     [0] (var->0-service variable)
                     (var->service variable))))))

(defn- var->factory-meta-deps
  "for multimethods"
  [variable]
  (if-some [deps (some-> variable meta ::deps (zipmap (repeat :required)))]
    (service-factory variable deps)))

(defn- var->factory-default [variable]
  @variable)

(defn- var->factory [variable]
  (-> (?? (var->factory-meta-deps variable)
          (var->factory-defn variable)
          (var->factory-default variable))
      (update-description assoc ::variable variable)))

(extend-protocol p/Factory
  nil
  (dependencies [_] nil)
  (build [_ _ _] nil)
  (description [this]
    {::kind :trivial
     :object nil})

  Object
  (dependencies [_] nil)
  (build [this _ _] this)
  (description [this]
    {::kind  :trivial
     :object this}))


(c/derive ::root     ::instance)
(c/derive ::template ::instance)
(c/derive ::service  ::instance)

(defmethod print-method ::instance [o ^Writer w]
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
            (build [_ deps _]
              (some-> key-name deps parser))
            (description [_]
              {::kind      :middleware
               :middleware ::env-parsing
               :cmap       cmap}))
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
            (build [_this deps _]
              (update-keys deps #(-> % name keyword)))
            (description [_]
              {::kind      :middleware
               :middleware ::ns-publics
               :ns         component-ns})))
        (registry key)))))

(defmacro with-open
  "A `c/with-open` variant that supports destructuring in bindings.

  bindings => [name init ...]
  Evaluates body in a try expression with names bound to the values
  of the inits, and a finally clause that calls (.close name) on each
  name in reverse order."
  {:clj-kondo/lint-as 'clojure.core/with-open}
  [bindings & body]
  {:pre [(vector? bindings)
         (even? (count bindings))]}
  (if (zero? (count bindings))
    `(do ~@body)
    (let [[binding-form init-expr] (subvec bindings 0 2)]
      `(let [resource# ~init-expr]
         (try
           (let [~binding-form resource#]
             (with-open ~(subvec bindings 2)
               ~@body))
           (finally
             (.close resource#)))))))

(defn log
  "A logging middleware.
  Calls `:after-build!` and `:after-demolish!` during `di/start`.
  Must be the last one in the middleware chain.
  Both callbacks are expected to accept
  the following arg `{:keys [key object]}`."
  [& {:keys   [after-build! after-demolish!]
      #_#_:as opts
      :or     {after-build!    (fn no-op [_])
               after-demolish! (fn no-op [_])}}]
  (fn [registry]
    (fn [key]
      (let [factory (registry key)]
        (if (-> factory p/description ::implementation-detail)
          factory
          (reify p/Factory
            (dependencies [_]
              (p/dependencies factory))
            (build [_ deps add-stop]
              (let [obj (p/build factory deps add-stop)]
                (after-build! {:key key :object obj})
                (add-stop #(after-demolish! {:key key :object obj}))
                obj))
            (description [_]
              (assoc (p/description factory)
                     ::log  {:will-be-logged true
                             #_#_:opts       opts}))))))))

(defn- with-inspect [registry]
  (fn [key]
    (let [factory       (registry key)
          declared-deps (p/dependencies factory)
          description   (p/description factory)
          info          (into {}
                              (filter (fn [[k v]] (some? v)))
                              {:key          key
                               :dependencies (not-empty declared-deps)
                               :description  (not-empty description)})
          result        (if (::implementation-detail description)
                          []
                          [info])]
      (reify p/Factory
        (dependencies [_]
          declared-deps)
        (build [_ deps _]
          (into result
                (comp
                 (mapcat val)
                 (distinct))
                deps))))))

(defn inspect
  "Collects and returns a vector of keys along with their dependencies.
  Useful for inspecting enabled components and services.
  Evaluates all registries with middlewares applied.

  Expects the same arguments as `start` and returns a vector of keys with dependencies e.g.:

  ```clojure
  [{:key `root :dependencies {`foo :required `bar :optional}}
   {:key `foo}
   {:key `bar}]
  ```"
  [key & middlewares]
  (with-open [components (start* ::implicit-root
                                 [middlewares
                                  (implicit-root key)
                                  with-inspect])]
    @components))


(defn- first-mw? [registry]
  (identical? registry initial-registry))

(defn- add-factory-watch [factory f]
  (when-some [var (-> factory p/description ::variable)]
    ;; Every memoize instance has a new factory instance for a var.
    ;; It is ok to pass a factory as a key.
    (add-watch var factory (fn [_ _ _ _] (f)))))

(defn- remove-factory-watch [factory]
  (when-some [var (-> factory p/description ::variable)]
    (remove-watch var factory)))

(defn ->memoize
  "Returns a statefull middleware that memoizes all registry build accesses.

  To stop all memoized components use `(di/stop mem)`."
  ^AutoCloseable [& middlewares]
  (let [registry  (apply-middlewares initial-registry middlewares)
        factories (ConcurrentHashMap.)
        objs      (ConcurrentHashMap.)
        ctx       {:*stop-list (atom '())}
        add-stop  (->add-stop ctx)]
    (reify
      AutoCloseable
      (close [this]
        (doseq [[_ factory] factories]
          (remove-factory-watch factory))
        (.clear factories)
        (.clear objs)
        (try-stop-started ctx))
      Function
      (apply [this previous-registry]
        (when-not (first-mw? previous-registry)
          (throw (ex-info "A memoized registry should be the first middleware"
                          {:type ::wrong-memoized-registry-position})))
        (fn [key]
          (let [factory (.computeIfAbsent factories key
                                          (fn [_]
                                            (let [factory (registry key)]
                                              (add-factory-watch factory
                                                                 #(.remove factories key factory))
                                              factory)))]
            (reify p/Factory
              (dependencies [_]
                (p/dependencies factory))
              (build [_ deps _add-stop]
                (.computeIfAbsent objs [factory deps]
                                  (fn [_] (p/build factory deps add-stop))))
              (description [_]
                (p/description factory)))))))))
