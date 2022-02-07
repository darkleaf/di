(ns darkleaf.di.core
  (:require
   [darkleaf.di.impl.map-destructuring-parser :as md-parser])
  (:import
   [java.lang AutoCloseable Exception]
   [java.io FileNotFoundException]
   [clojure.lang IDeref IFn]))

(set! *warn-on-reflection* true)

(defprotocol Stoppable
  :extend-via-metadata true
  (stop [this]))

(deftype ObjectWrapper [obj stop-fn]
  AutoCloseable
  (close [_]
    (stop-fn))
  IDeref
  (deref [_]
    obj)
  IFn
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
  (invoke [_ a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 args]
    (obj     a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 args))
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

(defn- resolve-ident [{:keys [replacements *system]}
                      ident default]
  (?? (replacements ident)
      (@*system ident)
      (try-requiring-resolve ident)
      default))

(defn- deps-definition [variable]
  (let [definition (-> variable meta :arglists last first)]
    (cond
      (map? definition) definition
      (= '_ definition) {}
      :else (throw (ex-info "invalid var" {})))))

(declare instanciate)

(defn- var-deps [ctx variable]
  (let [definition     (deps-definition variable)
        ident->default (md-parser/parse definition)]
   (reduce-kv (fn [acc ident default]
                (assoc acc ident (instanciate ctx ident default)))
              {}
              ident->default)))

(defn- build-obj [{:as ctx, :keys [*system *breadcrumbs instrument]}
                  ident variable]
  (let [builder (instrument variable)
        deps    (var-deps ctx variable)
        obj     (cond
                  (keyword? ident) (builder deps)
                  (symbol? ident)  (partial builder deps)
                  :else            (throw (ex-info "b" {})))]
    (vswap! *system assoc ident obj)
    (vswap! *breadcrumbs conj obj)
    obj))

(defn- instanciate [ctx ident default]
  (let [x (resolve-ident ctx ident default)]
    (cond
      (var? x) (build-obj ctx ident x)
      (nil? x) (throw (ex-info "not-found" {}))
      :else x)))

(defn- instanciate* [{:as ctx, :keys [*breadcrumbs]}
                     ident]
  (try
    (instanciate ctx ident nil)
    (catch Exception ex
      (throw (ex-info "can't start"
                      {:type                     ::can't-start
                       :ident                    ident
                       :stack-of-started-objects @*breadcrumbs}
                      ex)))))

(defn- stop-system [{:keys [*breadcrumbs]}]
  (doseq [dep @*breadcrumbs]
    (stop dep)))

(defn ^ObjectWrapper start
  ([ident]
   (start ident {}))
  ([ident replacements]
   (start ident replacements identity))
  ([ident replacements instrument]
   (let [ctx     {:*system      (volatile! {})
                  :*breadcrumbs (volatile! '())
                  :replacements replacements
                  :instrument   instrument}
         obj     (instanciate* ctx ident)
         stop-fn (partial stop-system ctx)]
     (->ObjectWrapper obj stop-fn))))

(extend-protocol Stoppable
  nil
  (stop [_])
  Object
  (stop [_])
  AutoCloseable
  (stop [this]
    (.close this)))
