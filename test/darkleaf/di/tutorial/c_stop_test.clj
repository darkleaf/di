(ns darkleaf.di.tutorial.c-stop-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

;; To stop a value, you should teach DI how to do it
;; through the `p/Stoppable` protocol implementation.

;; In this test I implement the protocol through metadata.

(defn root [{::keys [*stopped?]}]
  (with-meta 'root
    {`p/stop (fn [_] (deliver *stopped? true))}))

(t/deftest stop-test
  (let [*stopped? (promise)]
    (with-open [root (di/start `root {::*stopped? *stopped?})]
      (t/is (= 'root @root)))
    (t/is (deref *stopped? 0 false))))
