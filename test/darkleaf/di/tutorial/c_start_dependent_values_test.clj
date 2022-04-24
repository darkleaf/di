(ns darkleaf.di.tutorial.c-start-dependent-values-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; DI uses map destructuring syntax to describe dependencies of a component.
;; https://clojure.org/guides/destructuring#_associative_destructuring

(defn root [{dep `dep}]
  [:root dep])

(defn dep []
  :dep)

(t/deftest start-root
  (let [system-root (di/start `root)]
    (t/is (= [:root :dep] @system-root))))

;; Dependencies are required by default

(defn root* [{dep `dep*}])

(t/deftest start-root*
  (t/is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Missing dependency darkleaf.di.tutorial.c-start-dependent-values-test/dep*"

                          (di/start `root*))))
