(ns darkleaf.di.update-key-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

(t/deftest bug--update-non-existent-key-test
  (t/is (thrown-with-msg?
         ExceptionInfo
         #"\ACan't update non-existent key :darkleaf.di.update-key-test/component-with-typo\z"
         (di/start ::component
                   {::component 1}
                   (di/update-key ::component-with-typo inc)))))

(t/deftest bug2--update-non-existent-key-test
  (t/is (thrown-with-msg?
         ExceptionInfo
         #"\ACan't update non-existent key :darkleaf.di.update-key-test/component-with-typo\z"
         (di/start ::component
                   {::component          1
                    ::not-used-component 2}
                   (di/update-key ::not-used-component inc)))))
