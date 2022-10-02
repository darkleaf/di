(ns darkleaf.di.tutorial.x-instrument-test
  (:require
   [clojure.test :as t]
   [clojure.spec.alpha :as s]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

;; Actually, the rest of the `di/start` arguments are middlewares.
;; Maps and lists are special cases of ones.
;; They a usefull for logging, schema validation, AOP, etc.

;; For this case we have `di/instrument` middleware.
;; https://en.wikipedia.org/wiki/Decorator_pattern

(s/def root vector?)
(s/def ::a keyword?)

(defn root [{::keys [a]}]
  [:root a])

(t/deftest ok-test
  (with-open [root (di/start `root
                             {::a :a}
                             (di/instrument s/assert*))]
    (t/is (= [:root :a] @root))))

(t/deftest fail-test
  (t/is (thrown-with-msg? ExceptionInfo
                          #"\ASpec assertion failed"
                          (di/start `root
                                    {::a 42}
                                    (di/instrument s/assert*)))))
