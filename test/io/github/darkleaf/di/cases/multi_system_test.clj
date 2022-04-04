(ns io.github.darkleaf.di.cases.multi-system-test
  (:require [clojure.test :as t]
            [io.github.darkleaf.di.core :as di]))

(defn shared [{}]
  (Math/random))

(defn server [{shared `shared}]
  shared)

(t/deftest multi-system-test
  (with-open [shared (di/start `shared)
              a      (di/start `server [{`shared @shared} di/ns-registry])
              b      (di/start `server [{`shared @shared} di/ns-registry])]
    (t/is (double? @a))
    (t/is (double? @b))
    (t/is (identical? @a @b))))
