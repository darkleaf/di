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
  (let [logs         (atom [])
        built!       (fn [key deps obj]
                       (swap! logs conj [:built key deps obj]))
        demolished!  (fn [key obj]
                       (swap! logs conj [:demolished key obj]))
        [a b c
         :as system] (di/start [`a `b `c]
                               (di/log built! demolished!))]
    (di/stop system)
    (t/is (= [[:built `a {} a]
              [:built `b {`a a} b]
              [:built `c {`b b} c]
              [:built ::di/implicit-root {`a a `b b `c c} [a b c]]
              [:demolished ::di/implicit-root [a b c]]
              [:demolished `c c]
              [:demolished `b b]
              [:demolished `a a]]
             @logs))))
