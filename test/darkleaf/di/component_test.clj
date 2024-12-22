(ns darkleaf.di.component-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

(defmacro try-catch [& body]
  `(try
     ~@body
     nil
     (catch Exception ex#
       ex#)))

(defn nil-component-0-arity
  {::di/kind :component}
  []
  nil)

(defn nil-component-1-arity
  {::di/kind :component}
  [-deps]
  nil)

(t/deftest nil-component-0-arity-test
  (let [ex (try-catch (di/start `nil-component-0-arity))]
    (t/is (= "An error during component build" (-> ex ex-message)))
    (t/is (= "A component fn should not return nil" (-> ex ex-cause ex-message)))))

(t/deftest nil-component-1-arity-test
  (let [ex (try-catch (di/start `nil-component-1-arity))]
    (t/is (= "An error during component build" (-> ex ex-message)))
    (t/is (= "A component fn should not return nil" (-> ex ex-cause ex-message)))))
