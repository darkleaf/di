(ns darkleaf.di.tutorial.l-registries-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Here we use undefined dependencies.

(defn value [{dep-a `dep-a
              dep-b `dep-b}]
  [:value dep-a dep-b])

;; To locally define or redefine a dependency we should use registries.

(t/deftest map-registry
  (with-open [root (di/start `value {`dep-a :a `dep-b :b})]
    (t/is (= [:value :a :b] @root)))

  (with-open [root (di/start `value {`value :replacement})]
    (t/is (= :replacement @root)))

  (with-open [root (di/start `value {`dep-a :a} {`dep-b :b})]
    (t/is (= [:value :a :b] @root)))

  ;; last wins
  (with-open [root (di/start `value
                             {`dep-a :a `dep-b :b}
                             {`dep-a :a' `dep-b :b'})]
    (t/is (= [:value :a' :b'] @root))))


;; To avoid using `(apply di/start ...)`,
;; we can use an seqable value as a single registry.
;; See `clojure.core/seqable?`.
(t/deftest seqable-registry
  (with-open [root (di/start `value [{`dep-a :a}
                                     [{`dep-b :b}]])]
    (t/is (= [:value :a :b] @root))))


;; todo
;; include .m-abstractions-test
;; include darkleaf.di.registries-test
