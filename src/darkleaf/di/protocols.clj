(ns darkleaf.di.protocols)

(defprotocol Stoppable
  :extend-via-metadata true
  (stop [this]
    "Stops the object. Returns nothing."))

(defprotocol Factory
  :extend-via-metadata true
  (dependencies [this]
    "Returns a map of a key and a dependency type.
    A type can be :required, :skipping-circular, or :optional.")
  (build [this dependencies]
    "Builds a stoppable object from dependencies."))
