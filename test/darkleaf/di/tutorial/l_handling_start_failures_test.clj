;; # Handling start failures

(ns darkleaf.di.tutorial.l-handling-start-failures-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.utils :refer [catch-some]]))

;; A real system can fail halfway through start: the database
;; came up, the queue worker came up, then the third component
;; threw. If nothing stops what was already built, you leak
;; resources — and some are non-shareable. A Jetty server holds
;; a port. If you cannot stop it, you cannot start another one
;; on the same port. The system reference is usually lost too,
;; so the only escape is to restart the REPL.

;; DI handles this for you. When a build fails, it stops what
;; was already built, and only then propagates the error.

;; ## Built components are stopped before the error escapes

;; `dep` builds successfully and returns the `stopped` atom. Its
;; `::di/stop` flips the atom to `true`. `root` depends on `dep`
;; and then throws.

(defn dep
  {::di/stop (fn [stopped] (reset! stopped true))}
  [{stopped ::stopped}]
  stopped)

(defn root
  {::di/kind :component}
  [{_ `dep}]
  (throw (ex-info "build failed" {})))

;; `catch-some` captures whatever `di/start` throws. The original
;; failure is wrapped as `::di/build-failure`. `ex-cause` gives
;; you the original. `:stack` in `ex-data` shows the chain of
;; keys DI was building when it hit the gap.

(t/deftest built-deps-are-stopped-test
  (let [*stopped (atom false)
        ex       (catch-some (di/start `root {::stopped *stopped}))]
    (t/is (= ::di/build-failure (-> ex ex-data :type)))
    (t/is (= [`root] (-> ex ex-data :stack)))
    (t/is (= "build failed" (-> ex ex-cause ex-message)))
    ;; dep was stopped before the error propagated
    (t/is @*stopped)))

;; ## When a stop itself fails

;; The exception that triggered the failure is what `di/start`
;; throws. But during the cleanup that follows, the stop
;; functions themselves can throw, and DI does not want you to
;; lose either kind of information. The original error stays as
;; the main exception (reachable via `ex-cause`). Each stop
;; failure is attached as a *suppressed* exception (the JVM
;; mechanism — see `Throwable/.addSuppressed` and
;; `Throwable/.getSuppressed`).

(defn dep-stop-throws
  {::di/stop (fn [_] (throw (ex-info "stop failed" {})))}
  []
  :built)

(defn root-after-bad-stop
  {::di/kind :component}
  [{_ `dep-stop-throws}]
  (throw (ex-info "build failed" {})))

(t/deftest stop-errors-are-suppressed-test
  (let [ex (catch-some (di/start `root-after-bad-stop))]
    (t/is (= ::di/build-failure (-> ex ex-data :type)))
    (t/is (= "build failed" (-> ex ex-cause ex-message)))
    (t/is (= ["stop failed"] (->> ex .getSuppressed (map ex-message))))))

;; That is the end of the tutorial. The
;; [Example app](/doc/example.md) shows the pieces working
;; together in a real project.
