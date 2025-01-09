(ns darkleaf.di.tutorial.x-inspect-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn a []
  :ok)

(defn b [{a `a}]
  :ok)

(defn c [{a   `a
          b   `b
          :or {b :default}}]
  :ok)


(t/deftest zero-arity-service-test
  (t/is (= [{:key          ::di/implicit-root
             :dependencies {`a :required}}
            {:key         `a
             :description {:kind :service
                           :var  #'a}}]
           (di/inspect `a))))


;; todo: name
(t/deftest ok
  (t/is (= [{:key          ::di/implicit-root
             :dependencies {`c :required}}
            {:key          `c
             :dependencies {`a :required `b :optional}}
            {:key         `a
             :description {:kind     :service
                           :variable #'a}}
            {:key          `b
             :dependencies {`a :required}}]
           (di/inspect `c))))


(t/deftest update-key-test
  (t/is (= [{:key          ::di/implicit-root,
             :dependencies {`a :required}}
            {:key          `a,
             :dependencies {`a+di-update-key#0-target :optional,
                            `a+di-update-key#0-f      :optional}
             :description  {:kind       :middleware
                            :middleware ::di/update-key
                            :target     `a}}
            {:key         `a+di-update-key#0-target
             :description {:kind     :service
                           :variable #'a}}
            {:key `a+di-update-key#0-f}]
           (di/inspect `a (di/update-key `a str)))))
