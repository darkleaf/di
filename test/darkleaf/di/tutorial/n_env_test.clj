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

;; As of 2.3.0, there is `di/env-parsing` registry middleware
;; to parse values of environment variables.
;; You can define a dependency of env as a string key like \"PORT\",
;; and its value will be a string.
;; With this middleware, you can define it as a qualified keyword like :env.long/PORT,
;; and its value will be a number.

(defn jetty [{port :env.long/PORT
              :or  {port 8080}}]
  [:jetty port])

(t/deftest jetty-test
  (with-open [jetty (di/start `jetty
                              (di/env-parsing {:env.long parse-long}))]
    (t/is (= [:jetty 8080] @jetty)))
  (with-open [jetty (di/start `jetty
                              (di/env-parsing :env.long parse-long)
                              {"PORT" "8081"})]
    (t/is (= [:jetty 8081] @jetty))))
