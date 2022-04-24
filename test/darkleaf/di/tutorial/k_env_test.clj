(ns darkleaf.di.tutorial.k-env-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn value [{path "PATH"}]
  [:path path])

(t/deftest env-test
  (let [path (System/getenv "PATH")]
    (with-open [system-root (di/start `value)]
      (t/is (= [:path path] @system-root)))))
