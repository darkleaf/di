(ns darkleaf.di.tutorial.f-start-service-with-dependency-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

#_#_#_

(defn counter [{state `state}]
  (fn []
    (swap! state inc)))

(defn state []
  (atom 0))

(t/deftest start-counter
  (let [system-root (di/start `counter)]
    (t/is (= 1 (system-root)))
    (t/is (= 2 (system-root)))
    (t/is (= 3 (system-root)))))
