(ns ^:no-doc darkleaf.di.utils
  (:require
   [darkleaf.di.protocols :as p])
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

(defmacro try*
  "Macro to catch multiple exceptions with one catch body.

  Usage:
  (try*
    (println :a)
    (println :b)
    (catch* [A B] e (println (class e)))
    (catch C e (println :C))
    (finally (println :finally-clause)))

  Will be expanded to:
  (try
    (println :a)
    (println :b)
    (catch A e (println (class e)))
    (catch B e (println (class e)))
    (catch C e (println :C))
    (finally (println :finally-clause)))

  https://clojure.atlassian.net/browse/CLJ-2124
  https://github.com/Gonzih/feeds2imap.clj/blob/master/src/feeds2imap/macro.clj"
  [& body]
  (letfn [(catch*? [form]
            (and (seq form)
                 (= (first form) 'catch*)))
          (expand [[_catch* classes & catch-tail]]
            (map #(list* 'catch % catch-tail) classes))
          (transform [form]
            (if (catch*? form)
              (expand form)
              [form]))]
    (cons 'try (mapcat transform body))))

(defmacro catch-some [& body]
  `(try
     ~@body
     nil
     (catch Exception ex#
       ex#)))

(defn update-description [factory f & args]
  (reify
    p/Factory
    (dependencies [_]
      (p/dependencies factory))
    (build [_ deps]
      (p/build factory deps))
    (demolish [_ obj]
      (p/demolish factory obj))
    p/FactoryDescription
    (description [_]
      (apply f (p/description factory) args))))
