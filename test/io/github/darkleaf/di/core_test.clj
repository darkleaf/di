(ns io.github.darkleaf.di.core-test
  (:require
   [io.github.darkleaf.di.core :as di]
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


(defn logging-stub [log obj-name]
  (swap! log conj [:start obj-name])
  (reify di/Stoppable
    (stop [_]
      (swap! log conj [:stop obj-name]))))

(defn order-a [{::keys [log order-c]}]
  (logging-stub log :a))

(defn order-b [{::keys [log order-c]}]
  (logging-stub log :b))

(defn order-c [{::keys [log]}]
  (logging-stub log :c))

(defn order-root [{::keys [log order-a order-b]}]
  (logging-stub log :root))

(t/deftest order-test
  (let [log (atom [])]
    (with-open [obj (di/start ::order-root {::log log})])
    (t/is (= [[:start :c]
              [:start :a]
              [:start :b]
              [:start :root]

              [:stop :root]
              [:stop :b]
              [:stop :a]
              [:stop :c]]
             @log))))
