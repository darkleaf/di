(ns darkleaf.di.tutorial.u-wrap-transform-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest ok
  (with-open [root (di/start `root
                             {`root []
                              `a 1
                              `b 2}
                             (di/wrap di/transform `root conj (di/ref `a) (di/ref `b)))]
    (t/is (= [1 2] @root))))
