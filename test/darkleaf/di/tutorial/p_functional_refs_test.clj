(ns darkleaf.di.tutorial.p-functional-refs-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [clojure.spec.alpha :as s]))

;; In some cases, your components may have a complex structure or require transfromation.
;; You can use `di/ref` to transform a component.

;; The hard way

(defn port [{port "PORT"}]
  (Long/parseLong port))

(t/deftest port-test
  (with-open [root (di/start `port {"PORT" "8080"})]
    (t/is (= 8080 @root))))


;; The easy way

(def port* (-> (di/ref "PORT")
               (di/fmap parse-long)))

(t/deftest port-test
  (with-open [root (di/start `port* {"PORT" "8080"})]
    (t/is (= 8080 @root))))
