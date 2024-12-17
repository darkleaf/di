(ns darkleaf.di.component-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

(defn nil-component
  {::di/kind :component}
  []
  nil)

(t/deftest nil-component-test
  (t/is (thrown-with-msg?
         ExceptionInfo
         #"\Anil component #'darkleaf.di.component-test/nil-component\z"
         (di/start `nil-component))))
