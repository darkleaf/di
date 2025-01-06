(ns ^:no-doc darkleaf.di.utils
  (:import
   (java.util List)))

(set! *warn-on-reflection* true)

(defmacro ??
  ([] nil)
  ([x] x)
  ([x & next]
   `(if-some [x# ~x]
      x#
      (?? ~@next))))

(defn- index-of
  "Returns the index of the first occurrence of `x` in `xs`."
  [^List xs x]
  (if (nil? xs)
    -1
    (.indexOf xs x)))

(defn seq-contains? [xs x]
  (not (neg? (index-of xs x))))
