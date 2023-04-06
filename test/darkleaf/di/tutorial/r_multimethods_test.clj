(ns darkleaf.di.tutorial.r-multimethods-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defmulti service
  {::di/deps [::x]}
  (fn [-deps kind] kind))

(defmethod service :default [{x ::x} kind]
  [kind x])

(t/deftest required-dep-test
  (with-open [root (di/start `service {::x :value})]
    (t/is (= [:kind :value] (root :kind)))))


(defn- wrap-default [x default]
  (if (some? x) x default))

(def dep (-> (di/opt-ref ::optional)
             (di/fmap wrap-default :default-value)))

(t/deftest optional-dep-test
  (with-open [root (di/start `service {::x (di/ref `dep), ::optional :value})]
    (t/is (= [:kind :value] (root :kind))))

  (with-open [root (di/start `service {::x (di/ref `dep)})]
    (t/is (= [:kind :default-value] (root :kind)))))
