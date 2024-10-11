(ns darkleaf.di.dependency-types-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p])
  (:import
   (clojure.lang ExceptionInfo)))

(defn factory [dependency-key dependency-type]
  (reify p/Factory
    (dependencies [_]
      {dependency-key dependency-type})
    (build [_ deps]
      [dependency-key (get deps dependency-key)])
    (demolish [_ _])))

(t/deftest required-present-test
  (with-open [root (di/start ::root
                             {::root       (factory ::dependency :required)
                              ::dependency 42})]
    (t/is (= [::dependency 42] @root))))

(t/deftest missed-root-test
  (t/is (thrown-with-msg? ExceptionInfo
                          #"\AMissing dependency darkleaf.di.dependency-types-test/not-found\z"
                          (di/start `not-found))))

(t/deftest required-missed-test
  (t/is (thrown-with-msg? ExceptionInfo
                          #"\AMissing dependency :darkleaf.di.dependency-types-test/dependency\z"
                          (di/start `root
                                    {`root (factory ::dependency :required)}))))

(t/deftest required-circular-test
  (t/is (thrown-with-msg? ExceptionInfo
                          #"\ACircular dependency :darkleaf.di.dependency-types-test/root\z"
                          (di/start ::root
                                    {::root (factory ::root :required)}))))

(t/deftest optional-present-test
  (with-open [root (di/start ::root
                             {::root       (factory ::dependency :optional)
                              ::dependency 42})]
    (t/is (= [::dependency 42] @root))))

(t/deftest optional-missed-test
  (with-open [root (di/start ::root
                             {::root (factory ::dependency :optional)})]
    (t/is (= [::dependency nil] @root))))

(t/deftest optional-circular-test
  (t/is (thrown-with-msg? ExceptionInfo
                          #"\ACircular dependency :darkleaf.di.dependency-types-test/root\z"
                          (di/start ::root {::root (factory ::root :optional)}))))

(t/deftest unknown-test
  (t/is (thrown-with-msg? ExceptionInfo
                          #"\AUnknown dependency type encountered\z"
                          (di/start ::root
                                    {::root (factory ::dependency :unknown)}))))
