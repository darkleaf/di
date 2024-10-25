(ns darkleaf.di.update-key-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (java.util NoSuchElementException)))


(t/deftest ok-test
  (t/is (thrown? NoSuchElementException
                 (di/start ::a* {::a* 1} (di/update-key ::a inc)))))
