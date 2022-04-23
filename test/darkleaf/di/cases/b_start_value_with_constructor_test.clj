(ns darkleaf.di.cases.b-start-value-with-constructor-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn value []
  :my-value)

(t/deftest start-value
  (let [system-root (di/start `value)]
    (t/is (= :my-value @system-root))))
