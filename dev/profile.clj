(ns profile
  (:require
   [darkleaf.di.core :as di]
   [clj-async-profiler.core :as prof]))

(comment
  (prof/serve-ui 8080)

  (defn a
    {::di/kind :component}
    []
    :a)

  (prof/profile {}
    (dotimes [_ 10000]
      (di/start `a)))


  (prof/generate-diffgraph 1 2 {})
 ,,,)
