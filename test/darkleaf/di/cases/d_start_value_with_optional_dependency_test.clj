(ns darkleaf.di.cases.d-start-value-with-optional-dependency-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn root [{dep `dep
             :or {dep :default}}]
  [:root dep])

(t/deftest start-root
  (let [system-root (di/start `root)]
    (t/is (= [:root :default] @system-root))))
