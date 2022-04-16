(ns darkleaf.di.core
  (:refer-clojure :exclude [ref key])
  (:require
   [clojure.walk :as w]
   [darkleaf.di.destructuring-map :as map]
   [darkleaf.di.core :as di])
  (:import
   [java.lang AutoCloseable Exception]
   [java.io FileNotFoundException Writer]
   [clojure.lang IDeref IFn Var]))

(set! *warn-on-reflection* true)

(defprotocol Stoppable
  :extend-via-metadata true
  (stop [this]
    "Stops an object. Returns nothing."))

(defprotocol Factory
  :extend-via-metadata true
  (dependencies [this]
    "Returns a map of a key and a dependency type.
    A type can be :required, :skipping-circular, or :optional.")
  (build [this dependencies]
    "Builds a stoppable object from dependencies."))

(defmacro ^:private ??
  ([] nil)
  ([x] x)
  ([x & next]
   `(if-some [x# ~x]
      x#
      (?? ~@next))))

(def ^:private dependency-type-priority
  {:required          1
   :skipping-circular 2
   :optional          3})

(defn combine-dependencies
  "Combines dependencies. Use it with `reduce`.
  Dependencies are a hash map of a key and a dependency type."
  ([]
   {})
  ([a b]
   (merge-with (fn [x y]
                 (->> [x y]
                      (sort-by dependency-type-priority)
                      first))
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
    (if (= :skipping-circular dep-type)
      acc
      (circular-dependency! key))
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

(defn- build-obj [{:as ctx, :keys [registry *current-key *built-map *built-list]} key]
  (let [ctx           (update ctx :under-construction conj key)
        factory       (registry key)
        declared-deps (dependencies factory)
        resolved-deps (resolve-deps ctx declared-deps)
        obj           (build factory resolved-deps)]
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
    (try-run-all stop built-list)))

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

(defn- combine-registries [super registry]
  (cond
    (fn? registry)     (registry super)
    (vector? registry) (apply (first registry) super (rest registry))
    (map? registry)    (fn [key]
                         (?? (get registry key)
                             (super key)))
    (seq? registry) (reduce combine-registries
                            super registry)))

(defn- build-registry [registries]
  (reduce combine-registries
          nil-registry registries))

(defn- ns-registry
  "A registry looking for vars."
  [super]
  (fn [key]
    (?? (when (qualified-symbol? key)
          (try
            (requiring-resolve key)
            (catch FileNotFoundException _ nil)))
        (super key))))

(defn- env-registry
  "A registry looking for environment variables."
  [super]
  (fn [key]
    (?? (when (string? key)
          (System/getenv key))
        (super key))))

(defn ^AutoCloseable start
  "Starts system of dependent objects.

  The key argument is a name of the system root.
  Use symbols for var names, keywords for abstract dependencies,
  and strings for environments variables.

  The key is looked up in a registry.

  The registries argument is a list of registry conctructors.
  Each conctructor can be one of the following form:

  - a middleware-like function `super -> key -> Factory`
  - a vector of a function `(super args*) -> key -> Factory` and it's arguments
  - a map of key and `Factory`
  - a seq of the previous forms

  Super is a previous registry in the registries seq.

  Middleware-like functions are used to chain registries.
  This technique also allows you to instrument built objects.
  See `decorating-registry`.

  (di/start `root [{:my-abstraction implemntation
                    `some-key replacement
                    \"LOG_LEVEL\" \"info\"}
                   di/ns-registry
                   di/env-registry
                   [di/decorating-registry `log]])

  Returns a container contains started root of the system.
  The container implements `AutoCloseable`, `Stoppable`, `IDeref` and `IFn`.

  Use `with-open` in tests to stop the system reliably.

  See the tests for use cases."
  [key & registries]
  (let [registries (concat [env-registry ns-registry] registries)
        registry   (build-registry registries)
        ctx        {:*built-map         (volatile! {})
                    :*built-list        (volatile! '())
                    :under-construction #{}
                    :registry           registry}
        obj        (try-build ctx key)]
    ^{:type   ::root
      ::print obj}
    (reify
      Stoppable
      (stop [_]
        (some->> (stop-started ctx)
                 (reduce combine-throwable)
                 (throw)))
      AutoCloseable
      (close [this]
        (stop this))
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

(defn ref
  "A factory to refer to another one.

  (def port (di/ref \"PORT\" parse-long)

  (def routes (di/template [[\"/posts\" (di/ref `handler)]]))

  (di/start `root [{:my-abstraction (di/ref `implemntation)}
                   di/ns-registry])"
  ([key]
   (-> (ref key identity)
       (vary-meta assoc ::print key)))
  ([key f & args]
   ^{:type   ::ref
     ::print (vec (concat [key f] args))}
   (reify Factory
     (dependencies [_]
       {key true})
     (build [_ deps]
       (apply f (deps key) args)))))

(defn template
  "A factory to template a data-structure.
  Replaces `Factory` instances with built objects.

  (def routes (di/template [[\"/posts\" (di/ref `handler)]]))

  (def routes (di/template [[\"/posts\" #'handler]]))"
  [form]
  ^{:type   ::template
    ::print form}
  (reify Factory
    (dependencies [_]
      (->> form
           (tree-seq coll? seq)
           (map dependencies)
           (reduce combine-dependencies)))
    (build [_ deps]
      (w/postwalk #(build % deps) form))))

(defn decorating-registry
  "Wraps previous registries to decorate or instrument built objects.
  Use it for logging, schema checking, etc.
  The `decorator-key` can refer to a var like the following one.

  (defn my-instrumentation [{state :some/state} key object & args]
    (if (need-instrument? key object)
      (instrument state object args)
      object))

  (di/start `root [di/ns-registry
                   [di/decorating-registry `my-instrumentation arg1 arg2]])"
  [super decorator-key & args]
  (fn [key]
    (let [factory (super key)]
      (reify Factory
        (dependencies [_]
          (combine-dependencies (dependencies factory)
                                {decorator-key :skipping-circular}))
        (build [_ deps]
          (let [decorator (deps decorator-key)
                obj       (build factory deps)]
            (if decorator
              (apply decorator key obj args)
              obj)))))))

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

(extend-protocol Factory
  Var
  (dependencies [this]
    (if (defn? this)
      (dependencies-fn this)
      (dependencies @this)))
  (build [this deps]
    (if (defn? this)
      (build-fn this deps)
      (build @this deps)))

  nil
  (dependencies [_] nil)
  (build [_ _] nil)

  Object
  (dependencies [_] nil)
  (build [this _] this))

(extend-protocol Stoppable
  nil
  (stop [_])
  Object
  (stop [_]))

(derive ::root     ::reified)
(derive ::ref      ::reified)
(derive ::template ::reified)

(defmethod print-method ::reified [o ^Writer w]
  (.write w "#")
  (.write w (-> o type symbol str))
  (.write w " ")
  (binding [*out* w]
    (pr (-> o meta ::print))))
