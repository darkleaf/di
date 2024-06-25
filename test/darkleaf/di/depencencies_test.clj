(ns darkleaf.di.depencencies-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as dip]))

;;   root
;;  / \
;; a   b
;;  \ /
;;   c

(defn root
  {::di/stop #(swap! % conj [`root :stopped])}
  [{a `a, b `b, log ::log}]
  (swap! log conj [`root :built])
  log)

(defn a
  {::di/stop #(swap! % conj [`a :stopped])}
  [{c `c, log ::log}]
  (swap! log conj [`a :built])
  log)

(defn b
  {::di/stop #(swap! % conj [`b :stopped])}
  [{c `c, log ::log}]
  (swap! log conj [`b :built])
  log)

(defn c
  {::di/stop #(swap! % conj [`c :stopped])}
  [{log ::log}]
  (swap! log conj [`c :built])
  log)

(t/deftest order-test
  (let [log (atom [])]
    (-> (di/start `root {::log log})
        (di/stop))
    (t/is (= [[`c :built]
              [`a :built]
              [`b :built]
              [`root :built]

              [`root :stopped]
              [`b :stopped]
              [`a :stopped]
              [`c :stopped]]
             @log))))
