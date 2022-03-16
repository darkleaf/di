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
    "Returns a map of dependency key and `required?` flag")
  (build [this dependencies]))


(defmacro ^:private ??
  ([] nil)
  ([x] x)
  ([x & next]
   `(if-some [x# ~x]
      x#
      (?? ~@next))))


(defn- null-hook [key object]
  object)

(defn join-hooks [& hooks]
  (fn [key object]
    (reduce (fn [object hook] (hook key object))
            object
            hooks)))


(defn- or-fn [a b]
  (or a b))

(defn merge-dependencies [& deps]
  (apply merge-with or-fn deps))


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

(defn- join-throwable
  ([] nil)
  ([a] a)
  ([^Throwable a b]
   (.addSuppressed a b)
   a))

(defn- run-all! [proc coll]
  (some->> coll
           (map #(try-run proc %))
           (filter some?)
           (reduce join-throwable)
           (throw)))

(defn- stop-started! [{:keys [*started]}]
  (let [started @*started]
    (vswap! *started empty)
    (run-all! stop started)))

(defn- try-build [ctx key]
  (try
    (build-obj ctx key)
    (catch Throwable ex
      (let [stop-ex (try-run stop-started! ctx)
            ex      (->> [ex stop-ex]
                         (filter some?)
                         (reduce join-throwable))]
        (throw ex)))))

(defn- started-obj [ctx obj]
 ^{:type   ::started
   ::print obj}
 (reify
   AutoCloseable
   (close [_]
     (stop-started! ctx))
   IDeref
   (deref [_]
     obj)
   Factory
   (dependencies [_]
     nil)
   (build [_ _]
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
     (.applyTo ^IFn obj args))))

(defn ^AutoCloseable start
  ([key]
   (start key {}))
  ([key registry]
   (start key registry null-hook))
  ([key registry hook]
   (let [ctx {:*built       (volatile! {})
              :*started     (volatile! '())
              :registry     registry
              :hook         hook}
         obj (try-build ctx key)]
     (started-obj ctx obj))))


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
           (reduce merge-dependencies)))
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
       (reduce merge-dependencies)))

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


(derive ::started  ::-reified)
(derive ::ref      ::-reified)
(derive ::template ::-reified)

(defmethod print-method ::-reified [o ^Writer w]
  (.write w "#")
  (.write w (-> o type symbol str))
  (.write w " ")
  (binding [*out* w]
    (pr (-> o meta ::print))))
