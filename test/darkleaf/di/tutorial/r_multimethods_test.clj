;; # Multimethods

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.r-multimethods-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; You can use `defmulti` like `defn` to define a service.
;; Instead of `defn`, there is no way to get a definition of dependencies
;; and we have to define them as `::di/deps` on metadata.

(defmulti service
  {::di/deps [::x]}
  (fn [-deps kind] kind))

(defmethod service :default [{x ::x} kind]
  [kind x])

(t/deftest required-dep-test
  (with-open [root (di/start `service {::x :value})]
    (t/is (= [:kind :value] (root :kind)))))

;; `::di/deps` defines only required dependencies, mostly for simplicity.
;; If you need to use an optional dependency,
;; simply convert it to a required dependency by adding a default value.

(defn- wrap-default [x default]
  (if (some? x) x default))

(def dep (di/derive ::optional wrap-default :default-value))

(t/deftest optional-dep-test
  (with-open [root (di/start `service {::x (di/ref `dep), ::optional :value})]
    (t/is (= [:kind :value] (root :kind))))

  (with-open [root (di/start `service {::x (di/ref `dep)})]
    (t/is (= [:kind :default-value] (root :kind)))))
