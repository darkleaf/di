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
;; dependency optional, wrap it with `di/derive` and supply a
;; fallback:

(defn- wrap-default [x default]
  (if (some? x) x default))

(def dep (di/derive ::optional wrap-default :default-value))

(t/deftest optional-dep-test
  (with-open [root (di/start `service {::x (di/ref `dep), ::optional :value})]
    (t/is (= [:kind :value] (root :kind))))

  (with-open [root (di/start `service {::x (di/ref `dep)})]
    (t/is (= [:kind :default-value] (root :kind)))))
