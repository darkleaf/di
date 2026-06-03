;; # Tips

;; TODO: collection of small DI tricks and lesser-known features.
;; More to come — add new ones here as we run into them.

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

;; ## Group registries into one argument

;; A seqable value counts as a single registry argument — handy
;; when registries come from helper functions and you'd otherwise
;; need `(apply di/start ...)`.

(t/deftest grouped-registry-test
  ;; instead of:
  ;; (di/start ::root {::root :first} {::root :replacement})
  ;; ... pass them grouped:
  (with-open [r (di/start ::root [{::root :first}
                                  {::root :replacement}])]
    (t/is (= :replacement @r))))
