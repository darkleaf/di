;; # Transforming values

(ns darkleaf.di.tutorial.p-derive-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Sometimes the value DI builds for a key is not quite what
;; downstream code expects: an env var arrives as a string but
;; you want a number, a templated list contains nils you want
;; filtered out. `di/derive` builds a value from another key and
;; runs a function over the result. Shape:

;; ```clojure
;; (di/derive source-key f arg1 arg2 ...)
;; ;; ≡ (f source-value arg1 arg2 ...)
;; ```

;; ## A typed env var

;; The simplest case: parse an env var.

(def port (di/derive "PORT" parse-long))

(t/deftest port-test
  (with-open [root (di/start `port {"PORT" "8080"})]
    (t/is (= 8080 @root))))

;; Same effect as defining a one-line component:

(defn port'
  {::di/kind :component}
  [{port "PORT"}]
  (parse-long port))

(t/deftest port'-test
  (with-open [root (di/start `port' {"PORT" "8080"})]
    (t/is (= 8080 @root))))

;; Use whichever reads better. For env vars specifically,
;; [`di/env-parsing`](/doc/tutorial/n_env_test.md) is usually
;; nicer — it registers a parser once for a whole keyword
;; namespace (`:env.long`, `:env.json`). `di/derive` is the
;; general tool, useful when the transformation does not fit the
;; env-parsing pattern.

;; ## Post-processing a template

;; `di/derive` shines on top of a `di/template`. Here we build a
;; list of optional refs and filter out the nils:

(def -box (di/template [(di/opt-ref ::a)
                        (di/opt-ref ::b)
                        (di/opt-ref ::c)]))
(def box (di/derive `-box (partial filter some?)))

(t/deftest box-test
  (with-open [root (di/start `box {::b :b})]
    (t/is (= [:b] @root))))

;; The next chapter is what the whole tutorial has been pointing
;; at: composition through `di/update-key`. With it, modules
;; assemble into a system without naming each other.
