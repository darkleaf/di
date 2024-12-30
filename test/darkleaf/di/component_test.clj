(ns darkleaf.di.component-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.test-utils :refer [catch-some]]))

(defn nil-component-0-arity
  {::di/kind :component}
  []
  nil)

(defn nil-component-1-arity
  {::di/kind :component}
  [-deps]
  nil)

(t/deftest nil-component-0-arity-test
  (let [ex (catch-some (di/start `nil-component-0-arity))]
    (t/is (= "A component fn should not return nil" (-> ex ex-cause ex-message)))))

(t/deftest nil-component-1-arity-test
  (let [ex (catch-some (di/start `nil-component-1-arity))]
    (t/is (= "A component fn should not return nil" (-> ex ex-cause ex-message)))))
