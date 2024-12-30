(ns darkleaf.di.test-utils)

(defmacro catch-some [& body]
  `(try
     ~@body
     nil
     (catch Exception ex#
       ex#)))
