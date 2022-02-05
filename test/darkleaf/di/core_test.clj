(ns darkleaf.di.core-test
  (:require
   [darkleaf.di.core :as di]
   [clojure.test :as t]))

(defn obj-no-deps [{}]
  ::obj)

(t/deftest obj-no-deps-test
  (with-open [obj (di/start ::obj-no-deps)]
    (t/is (= ::obj @obj))))


(t/deftest no-obj-no-deps-test
  (with-open [obj (di/start ::missing {::missing ::obj})]
    (t/is (= ::obj @obj))))


(defn service-no-deps [{} arg]
  [::service arg])

(t/deftest service-no-deps-test
  (with-open [service (di/start `service-no-deps)]
    (t/is (= [::service ::arg] (service ::arg)))))


(t/deftest no-service-no-deps-test
  (with-open [service (di/start `missing {`missing (fn [] ::ok)})]
    (t/is (= ::ok (service)))))


(defn deps-root [{::syms [deps-a]}]
  [::root (deps-a)])

(defn deps-a [{::syms [deps-b]}]
  [::a (deps-b)])

(defn deps-b [{}]
  [::b])

(t/deftest deps-test
  (with-open [service (di/start `deps-root)]
    (t/is (= [::root [::a [::b]]] (service)))))
