;; # Derive

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.p-derive-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; In some cases, your components may have a complex structure or require transfromation.
;; You can use `di/derive` to transform a component.

;; The first way

(defn port
  {::di/kind :component}
  [{port "PORT"}]
  (parse-long port))

(t/deftest port-test
  (with-open [root (di/start `port {"PORT" "8080"})]
    (t/is (= 8080 @root))))


;; The second way

(def port' (di/derive "PORT" parse-long))

(t/deftest port'-test
  (with-open [root (di/start `port' {"PORT" "8080"})]
    (t/is (= 8080 @root))))


(def -box (di/template [(di/opt-ref ::a)
                        (di/opt-ref ::b)
                        (di/opt-ref ::c)]))
(def box (di/derive `-box (partial filter some?)))

(t/deftest box-test
  (with-open [root (di/start `box {::b :b})]
    (t/is (= [:b] @root))))
