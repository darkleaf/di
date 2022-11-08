(ns darkleaf.di.registries-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(def root 'root)

(t/deftest ns-registry-test
  (with-open [root (di/start `root)]
    (t/is (= 'root @root)))
  (t/is (thrown? Throwable (di/start `undefined))))

(t/deftest env-registry-test
  (with-open [root (di/start "PATH")]
    (t/is (= (System/getenv "PATH") @root)))
  (t/is (thrown? Throwable (di/start "DI_UNDEFINED"))))

(t/deftest map-registry-test
  (with-open [root (di/start `root {`root :stub})]
    (t/is (= :stub @root))))

(t/deftest sequential-registry-test
  (let [registries [{::a 1}
                    [{::b 2}]
                    [[{::c 3}]]]]
    (with-open [root (di/start ::a registries)]
      (t/is (= 1 @root)))
    (with-open [root (di/start ::b registries)]
      (t/is (= 2 @root)))
    (with-open [root (di/start ::c registries)]
      (t/is (= 3 @root)))))

(t/deftest nil-registry-test
  (with-open [root (di/start `root nil)]
    (t/is (= 'root @root))))
