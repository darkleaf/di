(ns io.github.darkleaf.di.cases.registry-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

;; Registry is the second argument of `di/start` function.
;; It should be a map of depencdency key and an instance of `di/Factory`.
;; A key can be a keyword, a symbol, or a string.


;; Registry can be used for configuration.
(defn use-global-config [{jdbc-url "JDBC_URL"
                          env      :env}]
  [::result jdbc-url env])

(t/deftest global-config-test
  (with-open [obj (di/start `use-global-config
                            {:env       :test
                             "JDBC_URL" "jdbc url"})]
    (t/is (= [::result "jdbc url" :test] @obj))))


;; Registry can be used to override dependencies.
(defn dependency [{}]
  ::dependency)

(defn object [{dep `dependency}]
  [::object dep])

(t/deftest stub-dep-test
  (with-open [obj (di/start `object)]
    (t/is (= [::object ::dependency] @obj)))
  (with-open [obj (di/start `object {`dependency ::stub})]
    (t/is (= [::object ::stub] @obj))))


;; Overrided deps should be stopped outside.
(t/deftest stub-is-not-managed-test
  (let [p    (promise)
        stub (reify di/Stoppable
               (stop [_]
                 (deliver p ::stopped)))
        obj  (di/start `object
                       {`dependency stub})]
    (di/stop obj)
    (t/is (not (realized? p)))
    (di/stop stub)
    (t/is (realized? p))))
