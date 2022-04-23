(ns darkleaf.di.cases.i-stop-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; To stop a value, you should teach DI how to do it
;; through the `di/Stoppable` protocol implementation.

;; In this test I implement the protocol through metadata.

;;   root
;;  / \
;; a   b
;;  \ /
;;   c

(def ^:dynamic *log*)

(defn root [{a `a, b `b}]
  (with-meta [:root a b]
    {`di/stop #'*log*}))

(defn a [{c `c}]
  (with-meta [:a c]
    {`di/stop #'*log*}))

(defn b [{c `c}]
  (with-meta [:b c]
    {`di/stop #'*log*}))

(defn c []
  (with-meta [:c]
    {`di/stop #'*log*}))

(t/deftest stop-order-test
  (let [log         (atom [])
        system-root (di/start `root)]
    (t/is (= [:root [:a [:c]] [:b [:c]]] @system-root))
    (binding [*log* #(swap! log conj %)]
      (di/stop system-root)
      (t/is (= [[:root [:a [:c]] [:b [:c]]]
                [:b [:c]]
                [:a [:c]]
                [:c]]
               @log)))))
