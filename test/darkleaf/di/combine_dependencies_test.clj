(ns darkleaf.di.combine-dependencies-test
  (:require
   [darkleaf.di.core :as di]
   [clojure.test :as t]))

(t/deftest combine-dependencies-test
  (t/are [expected input]
      (t/is (= expected (reduce di/combine-dependencies input)))
    {}
    []

    {:a :required}
    [{:a :required}]

    {:a :optional}
    [{:a :optional}]

    {:a :required, :b :required}
    [{:a :required} {:b :required}]

    {:a :optional, :b :required}
    [{:a :optional} {:b :required}]))
