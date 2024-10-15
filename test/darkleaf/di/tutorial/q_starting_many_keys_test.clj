;; # Starting many keys

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.q-starting-many-keys-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [darkleaf.di.core :as di]
   [clojure.test :as t]))

;; The standard `with-open` does not support destructuring in bindings.
;; Use `di/with-open` to handle resources with destructuring support.

(def a :a)
(def b :b)

(t/deftest verbose-test
  (di/with-open [[a b] (di/start ::root {::root (di/template [(di/ref `a) (di/ref `b)])})]
    (t/is (= :a a))
    (t/is (= :b b))))

;; The root container implements `clojure.lang.Indexed`
;; so you can use destructuring without derefing the root.

(t/deftest indexed-test
  (di/with-open [[a b] (di/start [`a `b])]
    (t/is (= :a a))
    (t/is (= :b b))))

;; The root container implements `clojure.lang.ILookup`
;; so you can use destructuring without derefing the root.

(t/deftest lookup-test
  (di/with-open [{:keys [a b]} (di/start {:a `a :b `b})]
    (t/is (= :a a))
    (t/is (= :b b))))
