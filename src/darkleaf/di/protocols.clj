(ns darkleaf.di.protocols)

(defprotocol Stoppable
  :extend-via-metadata true
  (stop [this]
    "Stops the object. Returns nothing."))

;; internal

(defprotocol Ref
  (ref-key [this])
  (ref-type [this]))

(defprotocol Factory
  (factory-build [this ctx]
    "Builds a stoppable object."))
