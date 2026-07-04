;; # Stopping components

(ns darkleaf.di.tutorial.c-stopping-components-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; By default, a component is built once at start and discarded on
;; stop — DI does nothing else with it. When a component owns a
;; resource (a connection, a thread pool, a file handle), attach
;; a stop function via metadata. DI calls it with the built value
;; when the system shuts down.

;; ## Declaring a stop function

;; `::di/stop` is just another function attached as metadata. It
;; receives the built value and its return value is ignored. Below,
;; the component returns an atom, and the stop function flips it to
;; `true` — the test asserts the flip happened.

;; Note that `root` below has no `{::di/kind :component}` mark.
;; A function with `::di/stop` metadata is a component by
;; convention — only components have a lifecycle, so there is no
;; need to state the kind explicitly.

(defn root
  {::di/stop #(reset! % true)}
  [{::keys [*stopped?]}]
  *stopped?)

(t/deftest stop-test
  (let [*stopped? (atom false)]
    (with-open [root (di/start `root {::*stopped? *stopped?})]
      (t/is (= false @@root)))
    (t/is @*stopped?)))

;; ## Stopping Java objects

;; In real systems, a stateful component is often a Java object —
;; a connection pool, a server, a queue — with a `close` or
;; `shutdown` method. Use a qualified method value (Clojure 1.12+)
;; or `memfn` to call it without reflection:

;; ```clojure
;; (defn connection-pool
;;   {::di/stop ConnectionPool/.close}
;;   [{max-conn :env.long/MAX_CONN}]
;;   (ConnectionPool. max-conn))
;;
;; (defn connection-pool
;;   {::di/stop (memfn ^ConnectionPool close)}
;;   [{max-conn :env.long/MAX_CONN}]
;;   (ConnectionPool. max-conn))
;; ```

;; The next chapter covers the REPL workflow — redefining functions
;; on a running system without restarting.
