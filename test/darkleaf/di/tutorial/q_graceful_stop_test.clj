(ns darkleaf.di.tutorial.q-graceful-stop-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

;; The DI tries to stop services that are already started
;; if another service fails while it is starting.

;; It throws with original exception.
;; All other possible exceptions are added as suppressed.

(defn root [{dep              `dep
             on-start-root-ex ::on-start-root-ex}]
  (throw on-start-root-ex))

(defn dep [{on-stop-dep-ex ::on-stop-dep-ex}]
  (reify p/Stoppable
    (stop [_]
      (throw on-stop-dep-ex))))

(t/deftest graceful-start-test
  (let [on-start-root-ex (ex-info "on start root" {})
        on-stop-dep-ex   (ex-info "on stop dep" {})
        registry         {::on-start-root-ex on-start-root-ex
                          ::on-stop-dep-ex   on-stop-dep-ex}

        ex (try
             (di/start `root registry)
             (catch Throwable ex
               ex))]

    (t/is (= on-start-root-ex ex))
    (t/is (= [on-stop-dep-ex] (vec (.getSuppressed ex))))))
