(ns ^:no-doc darkleaf.di.ref
  (:require
   [darkleaf.di.core :as-alias di]
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
  (build [_ deps _]
    (deps key))
  (description [_]
    {::di/kind :ref
     :key      key
     :type     type}))

(defmethod print-method Ref [o ^Writer w]
  (.write w "#darkleaf.di.core/")
  (.write w (case (:type o)
              :required "ref"
              :optional "opt-ref"))
  (.write w " ")
  (.write w (-> o :key str)))
