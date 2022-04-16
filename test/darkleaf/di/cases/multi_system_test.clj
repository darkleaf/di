(ns darkleaf.di.cases.multi-system-test
  (:require [clojure.test :as t]
            [darkleaf.di.core :as di]))

(defn shared [{}]
  (Math/random))

(defn server [{shared `shared}]
  shared)

(t/deftest multi-system-test
  (with-open [shared (di/start `shared)
              a      (di/start `server [{`shared @shared}])
              b      (di/start `server [{`shared @shared}])]
    (t/is (double? @a))
    (t/is (double? @b))
    (t/is (identical? @a @b))))
