;; # Side dependencies

(ns darkleaf.di.how-to.side-dependencies-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Some components must run at system start, but nothing else
;; references them. Two middlewares pull such a component into
;; the system without forcing the root to declare it:
;; `di/prepend-side-dependency` builds it before the rest of the
;; system, `di/add-side-dependency` — after.

;; Migrations go in front — they must finish before the
;; components that use the database start:

(defn migrations
  {::di/kind :component}
  [{*migrated? ::*migrated?}]
  (reset! *migrated? true))

(defn root
  {::di/kind :component}
  []
  'root)

(t/deftest prepend-side-dependency-test
  (let [*migrated? (atom false)]
    (with-open [root (di/start `root
                               (di/prepend-side-dependency `migrations)
                               {::*migrated? *migrated?})]
      (t/is @*migrated?)
      (t/is (= 'root @root)))))

;; A cache warmup or a setup step that uses components built by
;; the rest of the system goes after — `di/add-side-dependency`.

;; ## Build order

;; DI builds prepended side dependencies first, then the root
;; with its dependencies, and then added side dependencies.
;; Inside each group, side dependencies are built in the order
;; they were added. The system stops in the reverse order.

;; When the order is a relation between two components, no
;; middleware is needed: declare the task as a dependency of the
;; component that needs it.

;; ## Composition

;; Each subsystem declares its setup inside its own registry, so
;; the top-level start never mentions it:

;; ```clojure
;; ;; users subsystem
;; (defn registry [_]
;;   [(di/prepend-side-dependency `migrations)])
;;
;; ;; main system composes subsystems
;; (di/start `app
;;           (users/registry flags)
;;           (orders/registry flags))
;; ```

;; See
;; [Composition with `update-key`](/doc/tutorial/k_composition_with_update_key_test.md)
;; for the broader pattern.
