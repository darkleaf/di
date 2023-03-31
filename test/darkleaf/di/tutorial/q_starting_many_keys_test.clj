(ns darkleaf.di.tutorial.q-starting-many-keys-test
  (:require
   [darkleaf.di.core :as di]
   [clojure.test :as t]))

(def a :a)
(def b :b)

(t/deftest ok-test
  (with-open [root (di/start [`a `b])]
    (let [[a b] root]
      (t/is (= :a a))
      (t/is (= :b b)))))
