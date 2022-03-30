(ns io.github.darkleaf.di.core
  (:refer-clojure :exclude [ref key])
  (:require
   [clojure.walk :as w]
   [io.github.darkleaf.di.destructuring-map :as map])
  (:import
   [java.lang AutoCloseable Exception]
   [java.io FileNotFoundException Writer]
   [clojure.lang IDeref IFn Var]))

(set! *warn-on-reflection* true)

(defprotocol Stoppable
  :extend-via-metadata true
  (stop [this]))

(defprotocol Factory
  :extend-via-metadata true
  (dependencies [this]
    "Returns a map of a key and a boolean flag.
    Keys with true flag are required.")
  (build [this dependencies]
    "Builds a stoppable object with it dependencies."))

(defmacro ^:private ??
  ([] nil)
  ([x] x)
  ([x & next]
   `(if-some [x# ~x]
      x#
      (?? ~@next))))

(defn combine-hooks
  "Combines hooks. Use it with `reduce`.

  A hook is a function of a key and an object that returns a wrapped object.
  Use hooks for object instrumentation.
  "
  ([]
   (fn [key obj]
     obj))
  ([a b]
   (fn [key obj]
     (->> obj
          (a key)
          (b key)))))

(defn combine-dependencies
  "Combines dependencies. Use it with `reduce`.

  Dependencies are a hash map of key and boolean flag.
  Keys with true flag are required."
  ([]
   {})
  ([a b]
   (merge-with #(or %1 %2) a b)))

(defn- combine-throwable
  "Combines throwables. Use it with `reduce`."
  ([] nil)
  ([^Throwable a b]
   (.addSuppressed a b)
   a))

(declare find-or-build)

(defn- resolve-deps [ctx deps]
  (reduce-kv (fn [acc key required?]
               (if-some [obj (find-or-build ctx key)]
                 (assoc acc key obj)
                 (when required?
                   (throw (ex-info (str "Missing dependency " key)
                                   {:type ::missing-dependency
                                    :key  key})))))
             {}
             deps))

(defn- find-obj [{:keys [*built]} key]
  (get @*built key))

(defn- try-requiring-resolve [key]
  (when (qualified-symbol? key)
    (try
      (requiring-resolve key)
      (catch FileNotFoundException _ nil))))

(defn- resolve-factory [{:keys [registry]} key]
  (?? (registry key)
      (try-requiring-resolve key)))

(defn- build-obj [{:as ctx, :keys [*current-key *started *built hook]} key]
  (let [factory       (resolve-factory ctx key)
        declared-deps (dependencies factory)
        resolved-deps (resolve-deps ctx declared-deps)
        obj           (build factory resolved-deps)
        _             (vswap! *started conj obj)
        obj           (hook key obj)
        _             (vswap! *built assoc key obj)]
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

(defn- stop-started [{:keys [*started]}]
  (let [started @*started]
    (vswap! *started empty)
    (try-run-all stop started)))

(defn- try-build [ctx key]
  (try
    (build-obj ctx key)
    (catch Throwable ex
      (let [exs (stop-started ctx)
            exs (into [ex] exs)]
        (->> exs
             (reduce combine-throwable)
             (throw))))))

(defn ^AutoCloseable start
  "Starts system of dependent objects.

  The key is a name of the system root.
  It can be a var name or should be added to the registry.
  Use keywords for var names, keywords for abstract dependencies,
  and strings for environments variables.

  The registry is a map of key and `Factory`.

  Hooks are a seq of functions to instrument built objects.
  A hook is a function of a key and an object that returns a new one.

  Returns a container with started root.
  The container implements `Stoppable`, `IDeref` and `IFn`.

  Use `with-open` in tests to stop the system reliably.

  See the tests for use cases."
  ([key]
   (start key {}))
  ([key registry]
   (start key registry []))
  ([key registry hooks]
   (let [hook (reduce combine-hooks hooks)
         ctx  {:*built   (volatile! {})
               :*started (volatile! '())
               :registry registry
               :hook     hook}
         obj  (try-build ctx key)]
     ^{:type   ::started-root
       ::print obj}
     (reify
       AutoCloseable
       (close [_]
         (some->> (stop-started ctx)
                  (reduce combine-throwable)
                  (throw)))
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
         (.applyTo ^IFn obj args))))))

(defn ref
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

(defn template [form]
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

(defn- defn? [variable]
  (-> variable meta :arglists seq boolean))

(defn- dependencies-fn [variable]
  (->> variable
       meta
       :arglists
       (map first)
       (filter map?)
       (map map/dependencies)
       (reduce combine-dependencies)))

(defn- build-fn [variable deps]
  (let [max-arity (->> variable meta :arglists (map count) (reduce max 0) long)]
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
  (build [this _] this)

  Object
  (dependencies [_] nil)
  (build [this _] this))

(extend-protocol Stoppable
  nil
  (stop [_])
  Object
  (stop [_])
  AutoCloseable
  (stop [this]
    (.close this)))

(derive ::started-root ::reified)
(derive ::ref          ::reified)
(derive ::template     ::reified)

(defmethod print-method ::reified [o ^Writer w]
  (.write w "#")
  (.write w (-> o type symbol str))
  (.write w " ")
  (binding [*out* w]
    (pr (-> o meta ::print))))
