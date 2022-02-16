(ns io.github.darkleaf.di.core
  (:refer-clojure :exclude [ref])
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
  (-dependencies [this])
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
    #{})
  (-build [_ _ _]
    obj)
  IFn
  (call [_]
    (.call ^IFn obj))
  (run [_]
    (.run ^IFn obj))
  (invoke [_]
    (obj))
  (invoke [_ a1]
    (obj     a1))
  (invoke [_ a1 a2]
    (obj     a1 a2))
  (invoke [_ a1 a2 a3]
    (obj     a1 a2 a3))
  (invoke [_ a1 a2 a3 a4]
    (obj     a1 a2 a3 a4))
  (invoke [_ a1 a2 a3 a4 a5]
    (obj     a1 a2 a3 a4 a5))
  (invoke [_ a1 a2 a3 a4 a5 a6]
    (obj     a1 a2 a3 a4 a5 a6))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7]
    (obj     a1 a2 a3 a4 a5 a6 a7))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19))
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20))
  (invoke [_   a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 args]
    (apply obj a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 args))
  (applyTo [_ args] (apply obj args)))

(alter-meta! #'->ObjectWrapper assoc :private true)

(defn- try-requiring-resolve [ident]
  (when (qualified-symbol? ident)
    (try
      (requiring-resolve ident)
      (catch FileNotFoundException _ nil))))

(defmacro ^:private ??
  ([] nil)
  ([x] x)
  ([x & next]
   `(if-some [x# ~x]
      x#
      (?? ~@next))))

(defn- resolve-ident [{:keys [registry *system]} ident]
  (?? (@*system ident)
      (registry ident)
      (try-requiring-resolve ident)))

(declare instantiate)

(defn- resolve-deps [ctx deps]
  (reduce (fn [acc ident]
            (assoc acc ident (instantiate ctx ident)))
          {}
          deps))

(defn- register-to-stop [{:keys [*breadcrumbs]} obj]
  (vswap! *breadcrumbs conj obj))

(defn- register-in-system [{:keys [*system]} ident obj]
  (vswap! *system assoc ident obj))

(defn- -instantiate [{:as ctx, :keys [hook]} ident]
  (try
    (let [factory (resolve-ident ctx ident)
          deps    (-dependencies factory)
          deps    (resolve-deps ctx deps)
          obj     (-build factory deps #(register-to-stop ctx %))
          obj     (hook ident obj)]
      (register-in-system ctx ident obj)
      obj)
    (finally)))

(defn- instantiate [{:as ctx, :keys [*breadcrumbs starting-ident]} ident]
  (try
    (-instantiate ctx ident)
    (catch Throwable ex
      (if (= ::can't-start (-> ex ex-data :type))
        (throw ex)
        (throw (ex-info "can't start"
                        {:type                     ::can't-start
                         :starting-ident           starting-ident
                         :failed-ident             ident
                         :stack-of-started-objects @*breadcrumbs}
                        ex))))))

(defn- stop-system [{:keys [*breadcrumbs]}]
  (doseq [dep @*breadcrumbs]
    (stop dep)))

(defn- null-hook [ident object]
  object)

(defn ^ObjectWrapper start
  ([ident]
   (start ident {}))
  ([ident registry]
   (start ident registry null-hook))
  ([ident registry hook]
   (let [ctx     {:starting-ident ident
                  :*system        (volatile! {})
                  :*breadcrumbs   (volatile! '())
                  :registry       registry
                  :hook           hook}
         obj     (instantiate ctx ident)
         stop-fn (partial stop-system ctx)]
     (->ObjectWrapper obj stop-fn))))

(defn ref
  ([ident]
   (ref ident identity))
  ([ident f & args]
   (reify Factory
     (-dependencies [_]
       #{ident})
     (-build [_ deps _]
       (apply f (deps ident) args)))))

(defn ref-map
  "Plays well with `update-keys`"
  ([idents]
   (ref-map idents identity))
  ([idents f & args]
   (reify Factory
     (-dependencies [_]
       (set idents))
     (-build [_ deps _]
       (apply f deps args)))))

(defn template [form]
  (reify Factory
    (-dependencies [_]
      (->> form
           (tree-seq coll? seq)
           (mapcat -dependencies)
           (set)))
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
       (mapcat md-parser/parse)
       (set)))

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
  (-dependencies [_]
    #{})
  (-build [this _ _]
    this)

  Object
  (-dependencies [_]
    #{})
  (-build [this _ _]
    this))

(extend-protocol Stoppable
  nil
  (stop [_])
  Object
  (stop [_])
  AutoCloseable
  (stop [this]
    (.close this)))

(defn join-hooks [& hooks]
  (fn [ident object]
    (reduce (fn [object hook] (hook ident object))
            object
            hooks)))
