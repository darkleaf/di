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

(defn- build-obj*
  "Handles highter order components."
  [ctx factory]
  (let [declared-deps (p/dependencies factory)
        resolved-deps (resolve-deps ctx declared-deps)
        obj           (p/build factory resolved-deps)]
    (if (identical? factory obj)
      obj
      (recur ctx obj))))

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

  The key argument is a name of the system root.
  Use symbols for var names, keywords for abstract dependencies,
  and strings for environments variables.

  The key is looked up in a registry.
  By default registry uses system env and clojure namespaces
  to resolve string and symbol keys, respectively.

  You can extend it with middlewares.
  Each middleware can be one of the following form:

  - a function `registry -> key -> Factory`
  - a map of key and `Factory` instance
  - a sequence of the previous forms

  Middlewares also allows you to instrument built objects.
  It's useful for logging, schema validation, AOP, etc.
  See `with-decorator`.

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

(defn stop [x]
  (p/stop x))

(defn ref
  "Returns a factory referencing to another one.

  (def port (di/ref \"PORT\" parse-long)

  (def routes (di/template [[\"/posts\" (di/ref `handler)]]))

  (di/start `root {:my-abstraction (di/ref `implemntation)})

  See `template`."
  ([key]
   (-> (ref key identity)
       (vary-meta assoc ::print key)))
  ([key f & args]
   ^{:type   ::ref
     ::print (vec (concat [key f] args))}
   (reify p/Factory
     (dependencies [_]
       {key :required})
     (build [_ deps]
       (apply f (deps key) args)))))

(defn opt-ref
  "Returns a factory referencing to another possible undefined factory.

  (def port (di/opt-ref \"PORT\" (fnil parse-log \"8080\")))
  (def port (di/opt-ref ::config get :port 8080))

   See `ref` and `template`."
  ([key]
   (-> (opt-ref key identity)
       (vary-meta assoc ::print key)))
  ([key f & args]
   ^{:type   ::opt-ref
     ::print (vec (concat [key f] args))}
   (reify p/Factory
     (dependencies [_]
       {key :optional})
     (build [_ deps]
       (apply f (deps key) args)))))

(defn template
  "Returns a factory for templating a data-structure.
  Replaces `Factory` instances with built objects.

  (def routes (di/template [[\"/posts\" (di/ref `handler)]]))

  (def routes (di/template [[\"/posts\" #'handler]]))

  See `ref`."
  [form]
  ^{:type   ::template
    ::print form}
  (reify p/Factory
    (dependencies [_]
      (->> form
           (tree-seq coll? seq)
           (map p/dependencies)
           (reduce combine-dependencies)))
    (build [_ deps]
      (w/postwalk #(p/build % deps) form))))

(defn bind [factory f & args]
  (reify p/Factory
    (dependencies [_]
      (p/dependencies factory))
    (build [_ deps]
      (apply f (p/build factory deps) args))))

(defn wrap
  ;; "Wraps registry to decorate or instrument built objects.
  ;; Use it for logging, schema checking, AOP, etc.
  ;; The `decorator-key` should refer to a var like the following one.

  ;; (defn my-instrumentation [{state :some/state} key object & args]
  ;;   (if (need-instrument? key object)
  ;;     (instrument state object args)
  ;;     object))

  ;; (di/start `root (di/with-decorator `my-instrumentation arg1 arg2))"
  [decorator & args]
  (fn [registry]
    (fn [key]
      (let [factory (registry key)]
        (reify p/Factory
          (dependencies [_]
            (p/dependencies factory))
          (build [_ deps]
            (apply decorator key (p/build factory deps) args)))))))

(defn transform [key object target-key f & args]
  (if (= target-key key)
    (bind (template args) #(apply f object %))
    object))

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
(derive ::ref      ::reified)
(derive ::opt-ref  ::reified)
(derive ::template ::reified)

(defmethod print-method ::reified [o ^Writer w]
  (.write w "#")
  (.write w (-> o type symbol str))
  (.write w " ")
  (binding [*out* w]
    (pr (-> o meta ::print))))
