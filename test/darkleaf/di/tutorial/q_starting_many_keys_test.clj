;; # Starting many keys

(ns darkleaf.di.tutorial.q-starting-many-keys-test
  (:require
   [darkleaf.di.core :as di]
   [clojure.test :as t]))

;; A single `di/start` can bring up several components at once —
;; a webserver and a worker queue and a scheduler in production,
;; or a few independent components a test wants to poke at.
;; Rather than writing an explicit root component that pulls all
;; of them in, hand `di/start` a vector or a map of keys
;; directly. The returned root supports the matching kind of
;; destructuring.

(def a :a)
(def b :b)

;; ## Vector — Indexed root

;; A vector of keys produces a root that implements
;; `clojure.lang.Indexed`. Sequence destructuring works directly
;; — use `di/with-open` (a drop-in replacement for
;; `clojure.core/with-open` that supports destructuring):

(t/deftest indexed-test
  (di/with-open [[a b] (di/start [`a `b])]
    (t/is (= :a a))
    (t/is (= :b b))))

;; ## Map — ILookup root

;; A map of label → key produces a root that implements
;; `clojure.lang.ILookup`. Associative destructuring works:

(t/deftest lookup-test
  (di/with-open [{:keys [a b]} (di/start {:a `a :b `b})]
    (t/is (= :a a))
    (t/is (= :b b))))

;; The next chapter shows how to wire components into plain data
;; — reitit routes, scheduler tables — with `di/template` and
;; `di/ref`.
