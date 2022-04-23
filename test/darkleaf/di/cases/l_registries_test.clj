(ns darkleaf.di.cases.l-registries-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Here we use undefined dependencies.

(defn value [{dep-a `dep-a
              dep-b `dep-b}]
  [:value dep-a dep-b])

;; To locally define dependency we should use registries
(t/deftest map-registry
  (with-open [system-root (di/start `value {`dep-a :a `dep-b :b})]
    (t/is (= [:value :a :b] @system-root)))

  (with-open [system-root (di/start `value {`dep-a :a} {`dep-b :b})]
    (t/is (= [:value :a :b] @system-root)))

  ;; last wins
  (with-open [system-root (di/start `value
                                    {`dep-a :a `dep-b :b}
                                    {`dep-a :a' `dep-b :b'})]
    (t/is (= [:value :a' :b'] @system-root))))

;; To avoid using `apply`, we can use sequencies, not vectors, as a single registry.
(t/deftest seq-registry
  (let [registry (list {`dep-a :a} {`dep-b :b})]
    (with-open [system-root (di/start `value registry)]
      (t/is (= [:value :a :b] @system-root)))))
