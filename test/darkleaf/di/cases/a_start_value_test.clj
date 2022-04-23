(ns darkleaf.di.cases.a-start-value-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(def value :my-value)

(t/deftest start-value
  (let [system-root (di/start `value)]
    (t/is (= :my-value @system-root))))
