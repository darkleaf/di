(ns io.github.darkleaf.di.core-test
  (:require
   [io.github.darkleaf.di.core :as di]
   [clojure.test :as t]))

(t/deftest combine-dependencies-test
  (t/are [expected input]
      (t/is (= expected (reduce di/combine-dependencies input)))
    {}
    []

    {:a :required}
    [{:a :required}]

    {:a :skip-circular}
    [{:a :skip-circular}]

    {:a :optional}
    [{:a :optional}]

    {:a :required, :b :required}
    [{:a :required} {:b :required}]

    {:a :optional, :b :required}
    [{:a :optional} {:b :required}]

    {:a :required}
    [{:a :optional} {:a :skip-circular} {:a :required}]

    {:a :skip-circular}
    [{:a :optional} {:a :skip-circular}]))
