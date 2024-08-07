;; # Stop

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.c-stop-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; To stop a component, you should teach DI how to do it.
;; Use `::di/stop` to define a stop function.

(defn root
  {::di/stop #(reset! % true)}
  [{::keys [*stopped?]}]
  *stopped?)

(t/deftest stop-test
  (let [*stopped? (atom false)]
    (with-open [root (di/start `root {::*stopped? *stopped?})]
      (t/is (= false @@root)))
    (t/is @*stopped?)))

;; In most cases, a component will be a Java class.
;; To prevent reflection calls use `memfn`
;; ```clojue
;; (defn- connection-manager
;;   {::di/stop (memfn ^AutoCloseable close)}
;;   [{max-conn :env.long/CONNECTION_MANAGER_MAX_CONN
;;     :or {max-conn 50}}]
;;   (ConnctionManager. max-conn))
;; ```
