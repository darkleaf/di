(ns darkleaf.di.tutorial.b-dependencies-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

;; DI uses map destructuring syntax to describe dependencies of a component.
;; https://clojure.org/guides/destructuring#_associative_destructuring

;; зависимости объявляются ключами. Ключи - это символы, кейворды и строки.

;; todo: как-то тут рассказать про все типы ключей

(defn root [{a      `a
             ::syms [b]
             :or    {b ::default}
             :as    deps}]
  [:root a b deps])

(def a ::a)

(t/deftest root-test
  (with-open [root (di/start `root)]
    (t/is (= [:root ::a ::default {`a ::a}] @root))))

;; позже рассмотрим реестры

(t/deftest root-with-extra-deps-test
  (with-open [root (di/start `root {`b ::b})]
    (t/is (= [:root ::a ::b {`a ::a `b ::b}] @root))))


;; Dependencies are required by default

(defn root' [{a `a'}]
  [::root a])

(t/deftest root'-test
  (t/is (thrown-with-msg? ExceptionInfo
                          #"Missing dependency darkleaf.di.tutorial.b-dependencies-test/a'"
                          (di/start `root'))))
