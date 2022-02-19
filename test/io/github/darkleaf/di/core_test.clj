(ns io.github.darkleaf.di.core-test
  (:require
   [io.github.darkleaf.di.core :as sut]
   [clojure.test :as t]))

(t/deftest merge-dependencies-test
  (t/are [expected input]
      (t/is (= expected (apply sut/merge-dependencies input)))
    nil
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
