(ns darkleaf.di.update-key-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

(t/deftest bug--update-non-existent-key-test
  (t/is (thrown? ExceptionInfo
                 (di/start ::component
                           {::component 1}
                           (di/update-key ::component-with-typo inc)))))
