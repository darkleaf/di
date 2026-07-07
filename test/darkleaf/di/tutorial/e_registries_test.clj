;; # Registries

(ns darkleaf.di.tutorial.e-registries-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; You already saw the registry — the map passed to `di/start`.
;; This chapter covers it in full. A registry tells DI what to
;; use for a given key — overriding what DI would otherwise
;; resolve from a var, or supplying a value for a key that has
;; no var at all.

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

;; To avoid splicing with `apply`, a sequential collection (see
;; `clojure.core/sequential?`) — a vector or a list — counts as a
;; single registry. DI walks the sequence as if you had passed
;; each entry separately.

(t/deftest sequential-registry-test
  (with-open [root (di/start `value [{`dep-a :a}
                                     [{`dep-b :b}]])]
    (t/is (= [:value :a :b] @root))))

;; A map is the simplest form of this argument. The API calls the
;; whole variadic argument `middlewares`, and it accepts more than
;; maps — most importantly a function that wraps the registry, which
;; is how built-ins like `di/env-parsing` and `di/update-key` work.
;; Later chapters call these arguments middleware. See
;; [The middleware argument](/doc/reference/middleware_argument.md)
;; for the full list.

;; The next chapter introduces keyword keys — a way to decouple
;; a component from any specific var.
