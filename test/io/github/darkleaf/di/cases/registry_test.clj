(ns io.github.darkleaf.di.cases.registry-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

;; Regsitry is the second argument of `di/start` function.
;; It should be `ifn?` maps an ident to an instance of `di/Factory`.
;; Ident can be a keyword, a symbol, or a string.

;; In most cases the registry is a map but sometimes it's useful to pass a function.
(t/deftest registry-logger-test
  (let [registry         {`object (di/ref `a)
                          `a      ::value}
        log              (atom [])
        logging-registry (fn [ident]
                           (swap! log conj ident)
                           (registry ident))]
    (with-open [obj (di/start `object logging-registry)]
      (t/is (= ::value @obj)))
    (t/is (= [`object `a]
             @log))))

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
