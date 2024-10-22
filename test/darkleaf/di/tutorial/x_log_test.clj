(ns darkleaf.di.tutorial.x-log-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn a
  {::di/kind :component}
  []
  :a)

(defn b [{a `a}]
  :b)

(defn c
  {::di/kind :component}
  [{b `b}]
  :c)

(t/deftest log
  (let [logs        (atom [])
        built!      (fn [key obj]
                      (swap! logs conj [:built      key (pr-str obj)]))
        demolished! (fn [key obj]
                      (swap! logs conj [:demolished key (pr-str obj)]))]
    (with-open [root (di/start `c (di/log built! demolished!))])
    (t/is (= [[:built `a ":a"]
              [:built `b
               "#darkleaf.di.core/service #'darkleaf.di.tutorial.x-log-test/b"]
              [:built `c ":c"]
              [:demolished `c ":c"]
              [:demolished `b
               "#darkleaf.di.core/service #'darkleaf.di.tutorial.x-log-test/b"]
              [:demolished `a ":a"]]
             @logs))))
