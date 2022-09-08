(ns darkleaf.di.tutorial.t-higher-order-component-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; (defn component [{enabled? `enabled?}]
;;   (-> (cond-> {}
;;         enabled? (assoc :feature (di/ref `feature)))
;;       di/template))

;; (t/deftest enabled-test
;;   (with-open [obj (di/start `component {`enabled? true
;;                                         `feature  :some-feature})]
;;     (t/is (= {:feature :some-feature} @obj))))

;; (t/deftest disabled-test
;;   (with-open [obj (di/start `component {`enabled? false})]
;;     (t/is (= {} @obj))))

(defn component
  {::di/enable-key ::enabled
   ::di/fallback   :fallback}
  [{dep ::dep}]
  [:component dep])

(t/deftest enable-test
  (with-open [obj (di/start `component
                            {::enabled true
                             ::dep     :dep})]
    (t/is (= [:component :dep] @obj))))

(t/deftest disable-test
  (with-open [obj (di/start `component
                            {::enabled false
                             ::dep     :dep})]
    (t/is (= :fallback @obj))))

(t/deftest not-found-enable-key-test)
