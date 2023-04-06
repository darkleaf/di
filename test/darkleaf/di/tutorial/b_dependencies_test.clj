;; # Dependencies

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.b-dependencies-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

;; DI uses associative destructuring syntax to define dependencies of a component.
;; https://clojure.org/guides/destructuring#_associative_destructuring

;; There is a mapping between keys and components.
;; A key can be symbol, keyword, or string.
;; In this chapter we'll use only symbols.

;; If we use symbols DI will try to resolve a component's var.

;; In the following example the `root` component depends on
;; the `a` and `b` and the `b` is optional.
;; You also can get all component dependencies by the `deps` binding.

(defn root [{a      `a
             ::syms [b]
             :or    {b ::default}
             :as    deps}]
  [:root a b deps])

(def a ::a)

(t/deftest root-test
  (with-open [root (di/start `root)]
    (t/is (= [:root ::a ::default {`a ::a}] @root))))

;; `di/start` can accepts additional arguments.
;; In the following example the argument is a map registry.
;; I use it to define local keys.
;; In general they are middlewares but I'll describe it later.

(t/deftest root-with-extra-deps-test
  (with-open [root (di/start `root {`b ::b})]
    (t/is (= [:root ::a ::b {`a ::a `b ::b}] @root))))

;; Dependencies are required by default.
;; There is no defenition of `a'` so DI will throw an exception.

(defn root' [{a `a'}]
  [::root a])

(t/deftest root'-test
  (t/is (thrown-with-msg? ExceptionInfo
                          #"Missing dependency darkleaf.di.tutorial.b-dependencies-test/a'"
                          (di/start `root'))))
