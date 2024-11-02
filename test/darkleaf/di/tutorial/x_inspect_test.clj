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

(t/deftest ok
  (t/is (= [{:key          ::di/implicit-root
             :dependencies {`c :required}
             :meta         {}}
            {:key          `c
             :dependencies {`a :required `b :optional}
             :meta         {}}
            {:key  `a
             :meta {}}
            {:key          `b
             :dependencies {`a :required}
             :meta         {}}]
           (di/inspect `c))))

(t/deftest meta-test
  (t/is (= [{:key          ::di/implicit-root,
             :dependencies {`a :required}
             :meta         {}}
            {:key          `a,
             :dependencies {`a+di-update-key#0-target :optional,
                            `a+di-update-key#0-f      :optional}
             :meta         {::di/middleware ::di/update-key
                            ::di/target     `a}}
            {:key  `a+di-update-key#0-target
             :meta {}}
            {:key  `a+di-update-key#0-f
             :meta {}}]
           (di/inspect `a (di/update-key `a str)))))
