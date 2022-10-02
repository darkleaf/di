(ns darkleaf.di.tutorial.x-add-side-dependency-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn root []
  'root)

(defn migrations [{::keys [*migrated?]}]
  (deliver *migrated? true))

(t/deftest add-side-dependency-test
  (let [*migrated? (promise)]
    (with-open [root (di/start `root
                               (di/add-side-dependency `migrations)
                               {::*migrated? *migrated?})]
      (t/is (= 'root @root)))
    (t/is (deref *migrated? 0 false))))
