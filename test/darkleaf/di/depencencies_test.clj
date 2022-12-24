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

(defn root [{a `a, b `b, log ::log}]
  (swap! log conj [`root :built])
  (reify dip/Stoppable
    (unwrap [_]
      [:root a b])
    (stop [_]
      (swap! log conj [`root :stopped]))))

(defn a [{c `c, log ::log}]
  (swap! log conj [`a :built])
  (reify dip/Stoppable
    (unwrap [_]
      [:a c])
    (stop [_]
      (swap! log conj [`a :stopped]))))

(defn b [{c `c, log ::log}]
  (swap! log conj [`b :built])
  (reify dip/Stoppable
    (unwrap [_]
      [:b c])
    (stop [_]
      (swap! log conj [`b :stopped]))))

(defn c [{log ::log}]
  (swap! log conj [`c :built])
  (reify dip/Stoppable
    (unwrap [_]
      [:c])
    (stop [_]
      (swap! log conj [`c :stopped]))))

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
