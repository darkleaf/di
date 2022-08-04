(ns darkleaf.di.tutorial.i-stop-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

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
    {`p/stop #'*log*}))

(defn a [{c `c}]
  (with-meta [:a c]
    {`p/stop #'*log*}))

(defn b [{c `c}]
  (with-meta [:b c]
    {`p/stop #'*log*}))

(defn c []
  (with-meta [:c]
    {`p/stop #'*log*}))

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
