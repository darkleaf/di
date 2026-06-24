;; # Registries

(ns darkleaf.di.tutorial.e-registries-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Earlier chapters used `di/start` with a map second argument
;; without naming it. That map is a *registry*. A registry tells
;; DI what to use for a given key — overriding what DI would
;; otherwise resolve from a var, or supplying a value for a key
;; that has no var at all.

;; The component below declares two dependencies that DI cannot
;; resolve on its own — `dep-a` and `dep-b` have no vars. The
;; registry fills them in.

(defn value
  {::di/kind :component}
  [{dep-a `dep-a
    dep-b `dep-b}]
  [:value dep-a dep-b])

;; ## A map of values by key

;; The simplest registry is a map. Each entry maps a key to the
;; value DI should use. Any key can be overridden — including the
;; root key itself.

(t/deftest map-registry-test
  ;; supply two undefined deps
  (with-open [root (di/start `value {`dep-a :a `dep-b :b})]
    (t/is (= [:value :a :b] @root)))
  ;; replace the root with a literal value
  (with-open [root (di/start `value {`value :replacement})]
    (t/is (= :replacement @root))))

;; ## Stacking registries — last wins

;; `di/start` takes any number of registries after the key. They
;; stack: a key is resolved in the right-most registry that
;; defines it.

(t/deftest stacked-registries-test
  ;; two registries together
  (with-open [root (di/start `value {`dep-a :a} {`dep-b :b})]
    (t/is (= [:value :a :b] @root)))
  ;; later wins
  (with-open [root (di/start `value
                             {`dep-a :a `dep-b :b}
                             {`dep-a :a' `dep-b :b'})]
    (t/is (= [:value :a' :b'] @root))))

;; ## Grouping registries with a sequence

;; To avoid splicing with `apply`, a seqable value (see
;; `clojure.core/seqable?`) counts as a single registry. DI walks
;; the sequence as if you had passed each entry separately.

(t/deftest seqable-registry-test
  (with-open [root (di/start `value [{`dep-a :a}
                                     [{`dep-b :b}]])]
    (t/is (= [:value :a :b] @root))))

;; The map form is one of several registry shapes — see
;; [Middleware types](/doc/reference/middleware_types.md)
;; for the full picture. The tutorial only needs the map form.

;; The next chapter introduces keyword keys — a way to decouple
;; a component from any specific var.
