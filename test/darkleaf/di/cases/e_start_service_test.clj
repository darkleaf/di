(ns darkleaf.di.cases.e-start-service-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; functions are just values
(def service (fn []
               :my-value))

(t/deftest start-service
  (let [system-root (di/start `service)]
    (t/is (= :my-value (@system-root)))
    ;; we don't have to deref system-root to call it
    (t/is (= :my-value (system-root)))))
