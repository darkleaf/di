(ns darkleaf.di.tutorial.c-start-dependent-values-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn root [{dep `dep}]
  [:root dep])

(defn dep []
  :dep)

(t/deftest start-root
  (let [system-root (di/start `root)]
    (t/is (= [:root :dep] @system-root))))
