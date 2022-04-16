(ns darkleaf.di.cases.depencencies-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;;   root
;;  / \
;; a   b
;;  \ /
;;   c

(defn root [{a `a, b `b}]
  [:root a b])

(defn a [{c `c}]
  [:a c])

(defn b [{c `c}]
  [:b c])

(defn c []
  [:c])

(defn with-logging [{log `log} key obj]
  (swap! log conj [key :built])
  (with-meta obj {`di/stop (fn [_]
                             (swap! log conj [key :stopped]))}))

(t/deftest order-test
  (let [log (atom [])]
    (with-open [obj (di/start `root
                              {`log log}
                              [di/decorating-registry `with-logging])]
      (t/is (= [:root [:a [:c]] [:b [:c]]] @obj)))
    (t/is (= [[`c :built]
              [`a :built]
              [`b :built]
              [`root :built]

              [`root :stopped]
              [`b :stopped]
              [`a :stopped]
              [`c :stopped]]
             @log))))
