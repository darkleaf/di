(ns io.github.darkleaf.di.core-test
  (:require
   [io.github.darkleaf.di.core :as di]
   [clojure.test :as t]))

(t/deftest combine-dependencies-test
  (t/are [expected input]
      (t/is (= expected (reduce di/combine-dependencies input)))
    {}
    []

    {:a true}
    [{:a true}]

    {:a false}
    [{:a false}]

    {:a true, :b true}
    [{:a true} {:b true}]

    {:a true}
    [{:a false} {:a true}]

    {:a true, :b true}
    [{:a true} {:b true}]))
