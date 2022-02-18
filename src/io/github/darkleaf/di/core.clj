(ns io.github.darkleaf.di.core
  (:refer-clojure :exclude [ref key])
  (:require
   [clojure.walk :as w]
   [io.github.darkleaf.di.impl.map-destructuring-parser :as md-parser])
  (:import
   [java.lang AutoCloseable Exception]
   [java.io FileNotFoundException]
   [clojure.lang IDeref IFn Var]))

(set! *warn-on-reflection* true)

(defprotocol Stoppable
  :extend-via-metadata true
  (stop [this]))

(defprotocol Factory
  :extend-via-metadata true
  (-dependencies [this]
    "Returns a map of dependency key and `required?` flag")
  (-build [this deps register-to-stop]))

(deftype ObjectWrapper [obj stop-fn]
  AutoCloseable
  (close [_]
    (stop-fn))
  IDeref
  (deref [_]
    obj)
  Factory
  (-dependencies [_]
    nil)
  (-build [_ _ _]
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
    (.applyTo ^IFn obj args)))

(alter-meta! #'->ObjectWrapper assoc :private true)

(defn- try-requiring-resolve [key]
  (when (qualified-symbol? key)
    (try
      (requiring-resolve key)
      (catch FileNotFoundException _ nil))))

(defmacro ^:private ??
  ([] nil)
  ([x] x)
  ([x & next]
   `(if-some [x# ~x]
      x#
      (?? ~@next))))

(defn- resolve-factory [{:keys [registry *system]} key]
  (?? (@*system key)
      (registry key)
      (try-requiring-resolve key)))

(declare instantiate)

(defn- resolve-deps [ctx deps]
  (reduce-kv (fn [acc key required?]
               (assoc acc key (instantiate ctx key)))
             {}
             deps))

(defn- missing-deps [declared resolved]
  (reduce-kv (fn [acc key required?]
               (if (and required? (-> key resolved nil?))
                 (conj acc key)
                 acc))
             #{}
             declared))

(defn- check-deps! [key declared resolved]
  (if-some [mkeys (seq (missing-deps declared resolved))]
    (throw (ex-info (str "Missing dependencies for " key)
                    {:type         ::missing-dependencies
                     :missing-keys mkeys}))))

(defn- register-to-stop [{:keys [*breadcrumbs]} obj]
  (vswap! *breadcrumbs conj obj))

(defn- register-in-system [{:keys [*system]} key obj]
  (vswap! *system assoc key obj))

(defn- -instantiate [{:as ctx, :keys [hook]} key]
  (let [factory       (resolve-factory ctx key)
        declared-deps (-dependencies factory)
        resolved-deps (resolve-deps ctx declared-deps)
        _             (check-deps! key declared-deps resolved-deps)
        obj           (-build factory resolved-deps #(register-to-stop ctx %))
        obj           (hook key obj)]
    (register-in-system ctx key obj)
    obj))

(defn- instantiate [{:as ctx, :keys [*breadcrumbs starting-key]} key]
  (try
    (-instantiate ctx key)
    (catch Throwable ex
      (if (= ::start-threw-exception (-> ex ex-data :type))
        (throw ex)
        (throw (ex-info (str "Error on key " key " when starting " starting-key)
                        {:type                     ::start-threw-exception
                         :starting-key             starting-key
                         :failed-key               key
                         :stack-of-started-objects @*breadcrumbs}
                        ex))))))

(defn- stop-system [{:keys [*breadcrumbs]}]
  (doseq [dep @*breadcrumbs]
    (stop dep)))

(defn- null-hook [key object]
  object)

(defn ^ObjectWrapper start
  ([key]
   (start key {}))
  ([key registry]
   (start key registry null-hook))
  ([key registry hook]
   (let [ctx     {:starting-key key
                  :*system      (volatile! {})
                  :*breadcrumbs (volatile! '())
                  :registry     registry
                  :hook         hook}
         obj     (instantiate ctx key)
         stop-fn (partial stop-system ctx)]
     (->ObjectWrapper obj stop-fn))))

(defn ref
  ([key]
   (ref key identity))
  ([key f & args]
   (reify Factory
     (-dependencies [_]
       {key true})
     (-build [_ deps _]
       (apply f (deps key) args)))))

(defn ref-map
  "Plays well with `update-keys`"
  ([keys]
   (ref-map keys identity))
  ([keys f & args]
   (reify Factory
     (-dependencies [_]
       (zipmap keys (repeat true)))
     (-build [_ deps _]
       (apply f deps args)))))

(defn template [form]
  (reify Factory
    (-dependencies [_]
      (->> form
           (tree-seq coll? seq)
           (map -dependencies)
           (reduce md-parser/merge-deps)))
    (-build [_ deps register-to-stop]
      (w/postwalk #(-build % deps register-to-stop)
                  form))))

(defn- -defn? [variable]
  (-> variable meta :arglists seq boolean))

(defn- -dependencies-fn [variable]
  (->> variable
       meta
       :arglists
       (map first)
       (filter map?)
       (map md-parser/parse)
       (reduce md-parser/merge-deps)))

(defn- allow-defaults [m]
  (reduce-kv (fn [acc k v]
               (if (some? v)
                 (assoc acc k v)
                 acc))
             {} m))

(defn- -build-fn [variable deps register-to-stop]
  (let [max-arity (->> variable meta :arglists (map count) (reduce max 0) long)
        deps      (allow-defaults deps)]
    (case max-arity
      0 (doto (variable)
          register-to-stop)
      1 (doto (variable deps)
          register-to-stop)
      (partial variable deps))))

(extend-protocol Factory
  Var
  (-dependencies [this]
    (if (-defn? this)
      (-dependencies-fn this)
      (-dependencies @this)))
  (-build [this deps register-to-stop]
    (if (-defn? this)
      (-build-fn this deps register-to-stop)
      (-build @this deps register-to-stop)))

  nil
  (-dependencies [_] nil)
  (-build [this _ _] this)

  Object
  (-dependencies [_] nil)
  (-build [this _ _] this))

(extend-protocol Stoppable
  nil
  (stop [_])
  Object
  (stop [_])
  AutoCloseable
  (stop [this]
    (.close this)))

(defn join-hooks [& hooks]
  (fn [key object]
    (reduce (fn [object hook] (hook key object))
            object
            hooks)))
