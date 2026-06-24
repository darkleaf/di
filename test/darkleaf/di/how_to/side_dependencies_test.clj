;; # Side dependencies

(ns darkleaf.di.how-to.side-dependencies-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Some components must run at system start, but nothing else
;; references them — database migrations are the classic case.
;; `di/add-side-dependency` pulls such a component into the
;; system without forcing the root to declare it as a dependency.

(defn root
  {::di/kind :component}
  []
  'root)

(defn migrations
  {::di/kind :component}
  [{::keys [*migrated?]}]
  (reset! *migrated? true))

(t/deftest add-side-dependency-test
  (let [*migrated? (atom false)]
    (with-open [root (di/start `root
                               (di/add-side-dependency `migrations)
                               {::*migrated? *migrated?})]
      ;; `migrations` ran as part of start...
      (t/is @*migrated?)
      ;; ...even though `root` does not reference it.
      (t/is (= 'root @root)))))

;; ## Why not just list it as another root?

;; You could — `di/start` takes a vector of keys and builds them
;; in the order you list them. Migrations must come first so
;; they run before the app:

;; ```clojure
;; (di/start [`migrations `root] ...)
;; ```

;; But then the start call has to enumerate every cross-cutting
;; concern in the right order — migrations before the app, and
;; so on. `add-side-dependency` lets each subsystem declare its
;; setup inside its own registry, so the top-level start stays
;; clean.

;; The usual pattern: applications are split into subsystems, and
;; each subsystem ships its own `registry` function that
;; contributes components and middlewares. A subsystem that owns
;; migrations declares its side dependency inside its own
;; registry. The root never mentions it.

;; ```clojure
;; ;; users subsystem
;; (defn registry [_]
;;   [(di/add-side-dependency `migrations)])
;;
;; ;; main system composes subsystems
;; (di/start `app
;;           (users/registry flags)
;;           (orders/registry flags))
;; ```

;; See
;; [Composition with `update-key`](/doc/tutorial/k_composition_with_update_key_test.md)
;; for the broader pattern.
