;; # Fmap

^{::clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.p-fmap-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as dip]
   [clojure.spec.alpha :as s]))

;; In some cases, your components may have a complex structure or require transfromation.
;; You can use `di/fmap` to transform a component.

;; The hard way

(defn port [{port "PORT"}]
  (parse-long port))

(t/deftest port-test
  (with-open [root (di/start `port {"PORT" "8080"})]
    (t/is (= 8080 @root))))


;; The easy way

(def port' (-> (di/ref "PORT")
               (di/fmap parse-long)))

(t/deftest port'-test
  (with-open [root (di/start `port' {"PORT" "8080"})]
    (t/is (= 8080 @root))))


(def box (-> (di/template [(di/opt-ref ::a)
                           (di/opt-ref ::b)
                           (di/opt-ref ::c)])
             (di/fmap (partial filter some?))))

(t/deftest box-test
  (with-open [root (di/start `box {::b :b})]
    (t/is (= [:b] @root))))
