;; # Multi system
^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.z-multi-system-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; In some cases, you may need multiple systems and sharing a subsystem between them.
;; In that case just pass specify the subsystem in registry.

;; To get a value of the subsystem, you should `deref` it as you would for a regular system root.
;; Also you should manually stop systems in reverse order.

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
