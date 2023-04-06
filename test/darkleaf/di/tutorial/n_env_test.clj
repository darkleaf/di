;; # Env

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.n-env-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Like symbols and keywords, you can also use strings for keys.
;; String keys are resolved into values of  environment variables.

(defn root [{path "PATH"}]
  [:root path])

(def PATH (System/getenv "PATH"))

(t/deftest root-test
  (with-open [root (di/start `root)]
    (t/is (= [:root PATH] @root))))
