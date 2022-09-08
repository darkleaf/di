(ns darkleaf.di.protocols)

(defprotocol Stoppable
  :extend-via-metadata true
  (stop [this]
    "Stops the object. Returns nothing."))

(defprotocol Factory
  :extend-via-metadata true
  (build [this ctx]
    "Builds a stoppable object."))
