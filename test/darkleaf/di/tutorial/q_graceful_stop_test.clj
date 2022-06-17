(ns darkleaf.di.tutorial.q-graceful-stop-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; The DI tries to stop services that are already started
;; if another service fails while it is starting.

;; It throws with original exception.
;; All other possible exceptions are added as suppressed.

(def on-start-root-ex    (ex-info "on start root" {}))
(def on-stop-dep-ex (ex-info "on stop dep" {}))

(defn root [{dep `dep}]
  (throw on-start-root-ex))

(defn dep []
  (with-meta 'reified-stoppable
    {`di/stop (fn [_]
                (throw on-stop-dep-ex))}))

(t/deftest graceful-start-test
  (let [ex   (try
               (di/start `root)
               (catch Throwable ex
                 ex))]
    (t/is (= on-start-root-ex ex))
    (t/is (= [on-stop-dep-ex] (vec (.getSuppressed ex))))))
