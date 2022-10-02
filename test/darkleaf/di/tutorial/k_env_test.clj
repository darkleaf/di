(ns darkleaf.di.tutorial.k-env-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn root [{path "PATH"}]
  [:root path])

(def PATH (System/getenv "PATH"))

(t/deftest root-test
  (with-open [root (di/start `root)]
    (t/is (= [:root PATH] @root))))
