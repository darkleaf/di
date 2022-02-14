(ns io.github.darkleaf.di.core
  (:refer-clojure :exclude [ref])
  (:require
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
  (-build [this ident deps register-to-stop]))

(deftype ObjectWrapper [obj stop-fn]
  AutoCloseable
  (close [_]
    (stop-fn))
  IDeref
  (deref [_]
    obj)
  Factory
  (-dependencies [_]
    {})
  (-build [_ _ _ _]
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
  (when (qualified-ident? ident)
    (try
      (requiring-resolve (symbol ident))
      (catch FileNotFoundException _ nil))))

(defmacro ^:private ??
  ([] nil)
  ([x] x)
  ([x & next]
   `(if-some [x# ~x]
      x#
      (?? ~@next))))

(defn- resolve-ident [{:keys [registry *system]}
                      ident default]
  (?? (@*system ident)
      (registry ident)
      (try-requiring-resolve ident)
      default
      (throw (ex-info "not-found" {:ident ident}))))

(declare instanciate)

(defn- resolve-deps [ctx deps]
  (reduce-kv (fn [acc ident default]
               (assoc acc ident (instanciate ctx ident default)))
             {}
             deps))

(defn- register-to-stop [{:keys [*breadcrumbs]} obj]
  (vswap! *breadcrumbs conj obj))

(defn- register-in-system [{:keys [*system]} ident obj]
  (vswap! *system assoc ident obj))

(defn- -instanciate [{:as ctx, :keys [hook]}
                     ident default]
  (let [factory (resolve-ident ctx ident default)
        deps    (-dependencies factory)
        deps    (resolve-deps ctx deps)
        obj     (-build factory ident deps #(register-to-stop ctx %))
        obj     (hook ident obj)]
    (register-in-system ctx ident obj)
    obj))

(defn- instanciate [{:as ctx, :keys [*breadcrumbs]}
                    ident default]
  (try
    (-instanciate ctx ident default)
    (catch Exception ex
      (throw (ex-info "can't start"
                      {:type                     ::can't-start
                       :ident                    ident
                       :stack-of-started-objects @*breadcrumbs}
                      ex)))))

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
   (let [ctx     {:*system      (volatile! {})
                  :*breadcrumbs (volatile! '())
                  :registry     registry
                  :hook         hook}
         obj     (instanciate ctx ident nil)
         stop-fn (partial stop-system ctx)]
     (->ObjectWrapper obj stop-fn))))

(defn ref
  ([ident]
   (ref ident identity))
  ([ident f & args]
   (reify Factory
     (-dependencies [_]
       {ident nil})
     (-build [_ _ deps _]
       (apply f (deps ident) args)))))

(defn ref-vec
  ([idents]
   (ref-vec idents identity))
  ([idents f & args]
   (reify Factory
     (-dependencies [_]
       (zipmap idents (repeat nil)))
     (-build [_ _ deps _]
       (apply f (mapv deps idents) args)))))

(defn ref-map
  ([idents]
   (ref-map idents identity))
  ([idents f & args]
   (reify Factory
     (-dependencies [_]
       (zipmap idents (repeat nil)))
     (-build [_ _ deps _]
       (apply f deps args)))))

(extend-protocol Stoppable
  nil
  (stop [_])
  Object
  (stop [_])
  AutoCloseable
  (stop [this]
    (.close this)))

(extend-protocol Factory
  Object
  (-dependencies [_]
    {})
  (-build [this _ _ _]
    this)

  Var
  (-dependencies [this]
    (let [definition (-> this meta :arglists last first)]
      (if (map? definition)
        (md-parser/parse definition)
        (throw (ex-info "invalid var" {:var this})))))
  (-build [this ident deps register-to-stop]
    (if (symbol? ident)
      (partial this deps)
      (doto (this deps)
        register-to-stop))))

(defn join-hooks [& hooks]
  (fn [ident object]
    (reduce (fn [object hook] (hook ident object))
            object
            hooks)))
