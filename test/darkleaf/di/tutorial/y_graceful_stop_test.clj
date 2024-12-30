;; # Graceful stop

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.y-graceful-stop-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.test-utils :refer [catch-some]]))

;; The DI tries to stop components that are already started
;; if another component fails while it is starting.

;; It throws with original exception.
;; All other possible exceptions are added as suppressed.

(defn root
  {::di/kind :component}
  [{dep              `dep
    on-start-root-ex ::on-start-root-ex}]
  (throw on-start-root-ex))

(defn dep
  {::di/stop (fn [on-stop-dep-ex] (throw on-stop-dep-ex))}
  [{on-stop-dep-ex ::on-stop-dep-ex}]
  on-stop-dep-ex)


(t/deftest graceful-start-test
  (let [on-start-root-ex (ex-info "on start root" {})
        on-stop-dep-ex   (ex-info "on stop dep" {})
        registry         {::on-start-root-ex on-start-root-ex
                          ::on-stop-dep-ex   on-stop-dep-ex}
        ex               (-> (di/start `root registry)
                             catch-some)]
    (t/is (= on-start-root-ex (ex-cause ex)))
    (t/is (= [on-stop-dep-ex] (vec (.getSuppressed ex))))))
