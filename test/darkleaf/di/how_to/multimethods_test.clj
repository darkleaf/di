;; # Multimethods

(ns darkleaf.di.how-to.multimethods-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; A `defmulti` can be a service, but DI cannot read its argument
;; list the way it reads a `defn`. Declare the dependencies in
;; metadata under `::di/deps`.

(defmulti service
  {::di/deps [::x]}
  (fn [-deps kind] kind))

(defmethod service :default [{x ::x} kind]
  [kind x])

(t/deftest required-dep-test
  (with-open [root (di/start `service {::x :value})]
    (t/is (= [:kind :value] (root :kind)))))

;; ## Optional dependencies

;; `::di/deps` only declares required dependencies. To make a
;; dependency optional, route it through
;; [`di/derive`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#derive).
;; A factory made by `di/derive` depends on its source key as
;; *optional*: DI applies the function to the built value, or to
;; `nil` when the key is not defined. Here `wrap-default` turns
;; that `nil` into a fallback value:

(defn- wrap-default [x default]
  (if (some? x) x default))

(def dep (di/derive ::optional wrap-default :default-value))

;; The multimethod still requires `::x`, so the registry binds
;; `::x` to `dep` with `di/ref`: the required dependency is
;; satisfied by the derived value, and the optional part lives
;; inside `dep`. See also
;; [Transforming values](/doc/tutorial/j_transforming_values_test.md).

(t/deftest optional-dep-test
  (with-open [root (di/start `service {::x (di/ref `dep), ::optional :value})]
    (t/is (= [:kind :value] (root :kind))))

  (with-open [root (di/start `service {::x (di/ref `dep)})]
    (t/is (= [:kind :default-value] (root :kind)))))
