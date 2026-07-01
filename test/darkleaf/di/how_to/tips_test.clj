;; # Tips

;; A collection of small DI tricks and lesser-known features.

(ns darkleaf.di.how-to.tips-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; ## `::di/stop` infers `:component`

;; You don't need to attach `{::di/kind :component}` if the
;; function already has `::di/stop` metadata. DI treats any
;; function with a stop hook as a component — there is no other
;; reasonable interpretation, since services don't have a built
;; value to stop.

(defn resource
  {::di/stop #(reset! % :stopped)}
  []
  (atom :running))

(t/deftest stop-implies-component-test
  (let [root (di/start `resource)
        a    @root]
    (t/is (= :running @a))
    (di/stop root)
    (t/is (= :stopped @a))))

;; ## Pass a vector instead of `apply`

;; When your registries come from a helper that returns a
;; collection, you might reach for `(apply di/start ...)`. You do
;; not need to. `di/start` treats a seqable value as a single
;; argument, so pass the collection directly.

(t/deftest grouped-registry-test
  ;; registries from a helper — instead of:
  ;;   (apply di/start ::root registries)
  ;; pass them as one vector:
  (with-open [r (di/start ::root [{::root :first}
                                  {::root :replacement}])]
    (t/is (= :replacement @r))))
