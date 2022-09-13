(ns darkleaf.di.core
  (:refer-clojure :exclude [ref key])
  (:require
   [clojure.walk :as w]
   [darkleaf.di.destructuring-map :as map]
   [darkleaf.di.protocols :as p])
  (:import
   (clojure.lang IDeref IFn Var)
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

(declare ref-deps ref-build)

(defn- build-obj*
  "Handles higher order components"
  [ctx factory]
  (let [declared-deps     (p/dependencies factory)
        resolved-deps     (resolve-deps ctx declared-deps)
        ref               (p/build factory resolved-deps)
        declared-ref-deps (ref-deps ref)
        resolved-ref-deps (resolve-deps ctx declared-ref-deps)
        obj               (ref-build ref resolved-ref-deps)]
    obj))

(defn- build-obj [{:as ctx, :keys [registry *current-key *built-map *built-list]} key]
  (let [ctx     (update ctx :under-construction conj key)
        factory (registry key)
        obj     (build-obj* ctx factory)]
    (vswap! *built-list conj obj)
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

(defn- stop-started [{:keys [*built-list]}]
  (let [built-list @*built-list]
    (vswap! *built-list empty)
    (try-run-all p/stop built-list)))

(defn- try-build [ctx key]
  (try
    (?? (build-obj ctx key)
        (missing-dependency! key))
    (catch Throwable ex
      (let [exs (stop-started ctx)
            exs (into [ex] exs)]
        (->> exs
             (reduce combine-throwable)
             (throw))))))

(defn- nil-registry [key]
  nil)

(defn- apply-middleware [registry middleware]
  (cond
    (fn? middleware)         (middleware registry)
    (map? middleware)        (fn [key]
                               (?? (get middleware key)
                                   (registry key)))
    (sequential? middleware) (reduce apply-middleware
                                     registry middleware)
    :else                    (throw (IllegalArgumentException. "Wrong middleware kind"))))

(defn- with-ns
  "Adds support to the registry for looking up vars."
  [registry]
  (fn [key]
    (?? (when (qualified-symbol? key)
          (try
            (requiring-resolve key)
            (catch FileNotFoundException _ nil)))
        (registry key))))

(defn- with-env
  "Adds support to the registry for looking up environment variables."
  [registry]
  (fn [key]
    (?? (when (string? key)
          (System/getenv key))
        (registry key))))

(defn ^AutoCloseable start
  "Starts a system of dependent objects.

  key is a name of the system root.
  Use symbols for var names, keywords for abstract dependencies,
  and strings for environments variables.

  key is looked up in a registry.
  By default registry uses Clojure namespaces and system env
  to resolve symbols and strings, respectively.

  You can extend it with middlewares.
  Each middleware can be one of the following form:

  - a function `registry -> key -> Factory`
  - a map of key and `p/Factory` instance
  - a sequence of the previous forms

  Middlewares also allows you to instrument built objects.
  It's useful for logging, schema validation, AOP, etc.
  See `wrap`, `update-key`.

  (di/start `root
            {:my-abstraction implemntation
             `some-key replacement
             \"LOG_LEVEL\" \"info\"}
            [dev-middlwares test-middlewares]
            (di/with-decorator `log))

  Returns a container contains started root of the system.
  The container implements `AutoCloseable`, `Stoppable`, `IDeref` and `IFn`.

  Use `with-open` in tests to stop the system reliably.

  See the tests for use cases."
  [key & middlewares]
  (let [middlewares (concat [with-env with-ns] middlewares)
        registry    (apply-middleware nil-registry middlewares)
        ctx         {:*built-map         (volatile! {})
                     :*built-list        (volatile! '())
                     :under-construction #{}
                     :registry           registry}
        obj         (try-build ctx key)]
    ^{:type   ::root
      ::print obj}
    (reify
      p/Stoppable
      (stop [_]
        (some->> (stop-started ctx)
                 (reduce combine-throwable)
                 (throw)))
      AutoCloseable
      (close [this]
        (p/stop this))
      IDeref
      (deref [_]
        obj)
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

;; у нее роли
;; 1. в template
;; 2.1. в реестрах
;; 2.2. в значениях var
;; 3. в bind
(defrecord Ref [key type]
  p/Factory
  (dependencies [_]
    {key type})
  (build [_ deps]
    (deps key)))

(alter-meta! #'->Ref assoc :private true)
(alter-meta! #'map->Ref assoc :private true)

;; в шаблонах нельзя использовать все фабрики
;; если испльзовать var, то будут не уникальные инстансы
;; плюс у меня все заточено на отображение ключа в объект
;; а тут получается отображение фабрики в объект,
;; а фабрики не получится сравнивать (?) т.к. reify.

(defn- ref-deps [object]
  (if (instance? Ref object)
    (p/dependencies object)
    nil))

(defn- ref-build [object deps]
  (if (instance? Ref object)
    (p/build object deps)
    object))

(defn ref
  "Returns a factory referencing to a key.

  (def port (di/ref \"PORT\"))
  (defn server [{port `port}] ...)

  (def routes (di/template [[\"/posts\" (di/ref `handler)]]))

  (di/start `root {::my-abstraction (di/ref `my-implementation)})

  See `template`, `opt-ref`, `bind`, `p/build`."
  [key]
  (->Ref key :required))

(defn opt-ref
  "Returns a factory referencing to a possible undefined key.
  Produces nil in that case.

  See `template`, `ref`, `bind`."
  [key]
  (->Ref key :optional))

(defn template
  "Returns a factory for templating a data-structure.
  Replaces `ref` or `opt-ref` instances with built objects.

  (def routes (di/template [[\"/posts\" (di/ref `handler)]]))

  See `ref` and `opt-ref`."
  [form]
  ^{:type   ::template
    ::print form}
  (reify p/Factory
    (dependencies [_]
      (->> form
           (tree-seq coll? seq)
           (map ref-deps)
           (reduce combine-dependencies)))
    (build [_ deps]
      (w/postwalk #(ref-build % deps) form))))

(defn bind
  "Applies f to an object that the factory produces.
  f accepts a built object and returns updated one.
  Also f can return `ref`, for example, to select an implementation.

  f should return a `p/Stoppable` object, which also stops the original object.

  (def port (-> (di/ref \"PORT\")
                (di/bind parse-long)))

  (def implementation (-> (di/ref \"IMPLEMENTATION_NAME\")
                          (di/bind #(case %
                                      \"one\" (di/ref ::one)
                                      \"two\" (di/ref ::two)
                                      (di/ref ::other)))))

  See `ref`, `template`."
  [factory f]
  (reify p/Factory
    (dependencies [_]
      (p/dependencies factory))
    (build [_ deps]
      (-> factory
          (p/build deps)
          (f)))))

(def ^:private key? (some-fn symbol? keyword? string?))


;; wrap-keys (?)
(defn wrap
  "A registry middleware for decorating or instrumenting built objects.
  Use it for logging, schema checking, AOP, etc.

  decorator and args are keys.
  Also decorator can be a function in term of `ifn?`.

  A resolved decorator is a function of [object key & args].
  decorator should return a `p/Stoppable` object, which also stops the original object.

  In complex cases f can return `ref` to implement higher order components. See `bind`.

  It is smart enough not to instrument decorator's dependencies with the same decorator
  to avoid circular dependencies.

  (defn stateful-instrumentaion [{state :some/state} object key arg1 arg2] ...)
  (di/start ::root (di/wrap `stateful-instrumentation `arg1 ::arg2 \"arg3\")))

  (defn stateless-instrumentaion [object key arg1 arg2 arg3] ...)
  (di/start ::root (di/wrap   stateless-instrumentation `arg1 ::arg2 \"arg3\"))
  (di/start ::root (di/wrap #'stateless-instrumentation `arg1 ::arg2 \"arg3\"))

  See `update-key`, `bind`."
  [decorator & args]
  {:pre [(or (key? decorator)
             (ifn? decorator))
         (every? key? args)]}
  (let [own-keys            (cond-> (set args)
                              (key? decorator) (conj decorator))
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
                (let [decorator (deps decorator decorator)
                      args      (map deps args)
                      obj       (p/build factory deps)]
                  (apply decorator obj key args))))))))))

(defn update-key
  "A registry middleware for updating built object.

  target is a key to update.
  f and args are keys.
  Also f can be a function in term of `ifn?`.

  f should return a `p/Stoppable` object, which also stops the original object.

  In complex cases f can return `ref` to implement higher order components. See `bind`.

  (def routes [])
  (def subsystem-routes (di/template [[\"/posts\" (di/ref `handler)]]))

  (di/start ::root (di/update-key `routes conj `subsystem-routes))

  See `update`, `wrap`, `bind`."
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
                (let [f    (deps f f)
                      args (map deps args)
                      obj  (p/build factory deps)]
                  (apply f obj args))))))))))

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

(defn- build-fn [variable deps]
  (let [max-arity (->> variable
                       arglists
                       (map count)
                       (reduce max 0) long)]
    (case max-arity
      0 (variable)
      1 (variable deps)
      (partial variable deps))))

(extend-protocol p/Factory
  Var
  (dependencies [this]
    (if (defn? this)
      (dependencies-fn this)
      (p/dependencies @this)))
  (build [this deps]
    (if (defn? this)
      (build-fn this deps)
      (p/build @this deps)))

  nil
  (dependencies [_] nil)
  (build [_ _] nil)

  Object
  (dependencies [_] nil)
  (build [this _] this))

(extend-protocol p/Stoppable
  nil
  (stop [_])
  Object
  (stop [_]))

(derive ::root     ::reified)
(derive ::template ::reified)

(defmethod print-method ::reified [o ^Writer w]
  (.write w "#")
  (.write w (-> o type symbol str))
  (.write w " ")
  (binding [*out* w]
    (pr (-> o meta ::print))))

(defmethod print-method Ref [o ^Writer w]
  (.write w "#darkleaf.di.core/")
  (.write w (case (:type o)
              :required "ref"
              :optional "opt-ref"))
  (.write w " ")
  (.write w (-> o :key str)))
