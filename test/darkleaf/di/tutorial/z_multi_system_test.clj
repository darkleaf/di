(ns darkleaf.di.tutorial.z-multi-system-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn shared []
  (Object.))

(defn server [{name   ::name
               shared `shared}]
  [name shared])

(t/deftest multi-system-test
  (with-open [shared (di/start `shared)
              a      (di/start `server {`shared @shared
                                        ::name  :a})
              b      (di/start `server {`shared @shared
                                        ::name  :b})]
    (t/is (= :a (first @a)))
    (t/is (= :b (first @b)))
    (t/is (identical? (second @a) (second @b)))))
