(ns darkleaf.di.next-id-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn a
  {::di/stop #(swap! % assoc :stop-id (di/*next-id*))}
  []
  (atom {:start-id (di/*next-id*)}))

(t/deftest a-test
  (let [root (di/start `a)]
    (di/stop root)
    (t/is (= {:start-id 1 :stop-id 2} @@root))))
