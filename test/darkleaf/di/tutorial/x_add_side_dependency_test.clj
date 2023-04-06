;; # Add side dependency

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.x-add-side-dependency-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Actually, the rest of the `di/start` arguments are middlewares.
;; Maps and collections are special cases of ones.
;; Middlewares allow us to implement extra features.

;; In this test I'll show you how to perform a side effect like a database migration.

(defn root []
  'root)

(defn migrations [{::keys [*migrated?]}]
  (reset! *migrated? true))

(t/deftest add-side-dependency-test
  (let [*migrated? (atom false)]
    (with-open [root (di/start `root
                               (di/add-side-dependency `migrations)
                               {::*migrated? *migrated?})]
      (t/is @*migrated?)
      (t/is (= 'root @root)))))
