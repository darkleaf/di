(ns darkleaf.di.tutorial.x-ns-publics-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; excluded
(def nil-component nil)

;; excluded
(def unbound-component)

(defn component
  {::di/kind :component}
  []
  :component)

(defn service [{component `component} arg]
  [component arg])

(t/deftest ok-test
  (with-open [system (di/start :ns-publics/darkleaf.di.tutorial.x-ns-publics-test
                               (di/ns-publics))]
    (t/is (map? @system))
    (t/is (= #{:component :service :ok-test}
             (set (keys @system))))
    (t/is (= :component (:component system)))
    (t/is (= [:component :my-arg] ((:service system) :my-arg)))))
