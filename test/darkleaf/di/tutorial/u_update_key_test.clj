(ns darkleaf.di.tutorial.u-update-key-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest ok
  (with-open [root (di/start `root
                             {`root []
                              `a 1
                              `b 2}
                             (di/update-key `root conj `a `b))]
    (t/is (= [1 2] @root))))
