;; # Starting many keys

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.q-starting-many-keys-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [darkleaf.di.core :as di]
   [clojure.test :as t]))

(def a :a)
(def b :b)

(t/deftest verbose-test
  (with-open [root (di/start ::root {::root (di/template [(di/ref `a) (di/ref `b)])})]
    (let [[a b] @root]
      (t/is (= :a a))
      (t/is (= :b b)))))

;; The root container implements `clojure.lang.Indexed`
;; so you can use destructuring without derefing the root.

(t/deftest ok-test
  (with-open [root (di/start [`a `b])]
    (let [[a b] root]
      (t/is (= :a a))
      (t/is (= :b b)))))
