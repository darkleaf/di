(ns darkleaf.di.depencencies-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

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

(defn with-logging [{log `log} obj key]
  (swap! log conj [key :built])
  (with-meta obj {`p/stop (fn [_]
                            (swap! log conj [key :stopped]))}))

(t/deftest order-test
  (let [log (atom [])]
    (with-open [root (di/start `root
                               {`log log}
                               (di/instrument `with-logging))]
      (t/is (= [:root [:a [:c]] [:b [:c]]] @root)))
    (t/is (= [[`c :built]
              [`a :built]
              [`b :built]
              [`root :built]

              [`root :stopped]
              [`b :stopped]
              [`a :stopped]
              [`c :stopped]]
             @log))))
