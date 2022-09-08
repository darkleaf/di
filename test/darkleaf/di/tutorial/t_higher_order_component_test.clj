(ns darkleaf.di.tutorial.t-higher-order-component-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

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

(t/deftest not-found-enable-key-test
  (t/is (thrown-with-msg? ExceptionInfo
                          #"\AMissing dependency :darkleaf.di.tutorial.t-higher-order-component-test/enabled\z"
                          (di/start `component))))
