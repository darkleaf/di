(ns ^:no-doc darkleaf.di.ref
  (:require
   [darkleaf.di.protocols :as p])
  (:import
   (java.io Writer)))

(set! *warn-on-reflection* true)

;; у нее роли
;; 1. в template
;; 2.1. в реестрах
(defrecord Ref [key type]
  p/Factory
  (dependencies [_]
    {key type})
  (build [_ deps]
    (deps key))
  (demolish [_ _]))

;; в шаблонах нельзя использовать все фабрики
;; если испльзовать var, то будут не уникальные инстансы
;; плюс у меня все заточено на отображение ключа в объект
;; а тут получается отображение фабрики в объект,
;; а фабрики не получится сравнивать (?) т.к. reify.

(defn deps [object]
  (if (instance? Ref object)
    (p/dependencies object)
    nil))

(defn build [object deps]
  (if (instance? Ref object)
    (p/build object deps)
    object))

(defmethod print-method Ref [o ^Writer w]
  (.write w "#darkleaf.di.core/")
  (.write w (case (:type o)
              :required "ref"
              :optional "opt-ref"))
  (.write w " ")
  (.write w (-> o :key str)))
