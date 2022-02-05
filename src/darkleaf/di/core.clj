(ns darkleaf.di.core
  (:require
   [darkleaf.di.impl.deps-parser :as deps-parser])
  (:import
   [java.lang AutoCloseable]
   [java.io FileNotFoundException]
   [clojure.lang IDeref]
   [clojure.lang IFn]))

#_(set! *warn-on-reflection* true)
(set! *warn-on-reflection* false)

(defmulti decorate (fn [key obj] key))

(defprotocol Stoppable
  :extend-via-metadata true
  (stop [this]))

;; name?
(deftype Component [instance breadcrumbs]
  AutoCloseable
  (close [_]
    (doseq [dep breadcrumbs]
      (stop dep)))
  IDeref
  (deref [_]
    instance)
  IFn
  (invoke [_]
    (instance))
  (invoke [_  a1]
    (instance a1))
  (invoke [_  a1 a2]
    (instance a1 a2))
  (invoke [_  a1 a2 a3]
    (instance a1 a2 a3))
  (invoke [_  a1 a2 a3 a4]
    (instance a1 a2 a3 a4))
  (invoke [_  a1 a2 a3 a4 a5]
    (instance a1 a2 a3 a4 a5))
  (invoke [_  a1 a2 a3 a4 a5 a6]
    (instance a1 a2 a3 a4 a5 a6))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7]
    (instance a1 a2 a3 a4 a5 a6 a7))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8]
    (instance a1 a2 a3 a4 a5 a6 a7 a8))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20))
  (invoke [_  a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 args]
    (instance a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 args))
  (applyTo [_ args] (apply instance args)))

(defmacro ^:private ??
  ([] nil)
  ([x] x)
  ([x & next]
   `(if-some [x# ~x]
      x#
      (?? ~@next))))

(defn- safe-requiring-resolve [ident]
  (if (simple-ident? ident)
    ;; only in registry
    (throw (ex-info "" {})))
  (try
    (requiring-resolve (symbol ident))
    (catch FileNotFoundException _)))

;;{ident -> default}
(defn- resolve-deps [v]
  {})

(defn- instanciate [ident default
                    registry instrument
                    *system *breadcrumbs]
  (let [x (?? (registry ident)
              (@*system ident)
              (safe-requiring-resolve ident)
              default
              (throw (ex-info "not-found" {})))]
    (if-not (var? x)
      x
      (let [deps (reduce-kv
                  (fn [acc ident default]
                    (assoc acc ident (instanciate ident default
                                                  registry instrument
                                                  *system *breadcrumbs)))
                  {}
                  (deps-parser/parse x))
            ;; вот этот кусок бы вынести или в функцию или в мультиметод
            obj  (cond
                   (keyword? ident) (x deps)
                   (symbol? ident)  (partial x deps)
                   :else            (throw (ex-info "b" {})))
            obj  (decorate ident obj)
            obj  (instrument ident obj)]
        (vswap! *system assoc ident obj)
        (vswap! *breadcrumbs conj obj)
        obj))))

;;(defn ^Component start
(defn start
  ([ident]
   (start ident {}))
  ([ident registry]
   (start ident registry (fn [ident value] value)))
  ([ident registry instrument]
   (let [*system      (volatile! {})
         *breadcrumbs (volatile! [])
         instance     (try
                        (instanciate ident nil
                                     registry instrument
                                     *system *breadcrumbs)
                        (finally
                          ;; протестить бы
                          ;; и надо ли?
                          (doseq [dep @*breadcrumbs]
                            (stop dep))))]
     (->Component instance @*breadcrumbs))))


(defmethod decorate :default [_ obj] obj)

(extend-protocol Stoppable
  Object
  (stop [_])
  AutoCloseable
  (stop [this]
    (.close this)))

;; оставить одну арность

;; проверять наличие ключей конфигов такая себе идея
;; ключи же могут быть зависимо-опциональными

;; (start ::foo {::bar override, ::buzz #'override})
;; @component, (.close component)
;; конфиги - это состояние


;; декораторы через мультиметод decorate
;; (defn start [name overrides-map instrument-fn])
;; с помощью instrument можно делать абстракции
;; а он тоже мультиметод

;; внешние ресурсы в registry закрываются самостоятельно

;; компонент не может сам на себя ссылаться

"
# концепция
есть var, у нее первый аргумент - зависимости

символы мапятся на функции - (partial var ctx)
кейворды мапятся на конструкторы - (var ctx)

есть протокол - stop/halt
расширяемый через метаданные

зависимости - кейворды/символы с неймспейсом
кейворды - конфиги
"
