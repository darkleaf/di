(ns darkleaf.di.service-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn a [-deps]
  :a)

(defmulti b
  {::di/deps []}
  identity)

(t/deftest pr-test
  (with-open [root (di/start `a)]
    (t/is (= "#darkleaf.di.core/service #'darkleaf.di.service-test/a"
             (pr-str @root))))
  (with-open [root (di/start `b)]
    (t/is (= "#darkleaf.di.core/service #'darkleaf.di.service-test/b"
             (pr-str @root)))))
