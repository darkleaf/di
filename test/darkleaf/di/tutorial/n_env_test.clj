;; # Env

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.n-env-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Like symbols and keywords, you can also use strings for keys.
;; String keys are resolved into values of  environment variables.

(defn root
  {::di/kind :component}
  [{path "PATH"}]
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

(defn jetty
  {::di/kind :component}
  [{port :env.long/PORT
    :or  {port 8080}}]
  [:jetty port])

(defn required-env
  {::di/kind :component}
  [{enabled :env.bool/ENABLED}]
  [:enabled enabled])

(defn optional-env
  {::di/kind :component}
  [{enabled :env.bool/ENABLED
    :or {enabled true}}]
  [:enabled enabled])

(t/deftest jetty-test
  (with-open [jetty (di/start `jetty
                              (di/env-parsing {:env.long parse-long}))]
    (t/is (= [:jetty 8080] @jetty)))
  (with-open [jetty (di/start `jetty
                              (di/env-parsing :env.long parse-long)
                              {"PORT" "8081"})]
    (t/is (= [:jetty 8081] @jetty))))

(t/deftest env-test
  (t/is (thrown? clojure.lang.ExceptionInfo
                 (di/start `required-env
                           (di/env-parsing {:env.bool #(= "true" %)}))))

  (with-open [sys (di/start `required-env
                            (di/env-parsing {:env.bool #(= "true" %)})
                            {"ENABLED" "false"})]
    (t/is (= [:enabled false] @sys)))

  (with-open [sys (di/start `optional-env
                            (di/env-parsing {:env.bool #(= "true" %)}))]
    (t/is (= [:enabled true] @sys)))

  (with-open [sys (di/start `optional-env
                            (di/env-parsing {:env.bool #(= "true" %)})
                            {"ENABLED" "false"})]
    (t/is (= [:enabled false] @sys))))
