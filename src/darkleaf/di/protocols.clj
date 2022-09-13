(ns darkleaf.di.protocols)

(defprotocol Stoppable
  :extend-via-metadata true
  (stop [this]
    "Stops the object. Returns nothing."))

;; ;; :trivial - type for objects
;; (defprotocol Resolvable
;;   (resolvable-key [this])
;;   (resolvable-type [this]
;;     "A type can be :required, :optional, or :trivial"))

(defprotocol Factory
  (dependencies [this]
    "Returns a map of a key and a dependency type.
    A type can be :required or :optional.")
  (build [this dependencies]
    "Builds a stoppable object from dependencies."))
