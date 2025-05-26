(ns darkleaf.di.stop-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest nil-stop-test
  (t/is (nil? (di/stop nil))))

(t/deftest unbound-var-test
  (declare unbound-var)
  (t/is (nil? (di/stop unbound-var))))
