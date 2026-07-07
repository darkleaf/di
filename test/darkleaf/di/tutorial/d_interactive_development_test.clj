;; # Interactive development

(ns darkleaf.di.tutorial.d-interactive-development-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; In Clojure you grow a program by evaluating new code into a
;; running process. State — db connections, server threads, caches
;; — survives the eval, but the functions that capture that state
;; can grow stale. Stuart Sierra's *Reloaded Workflow* (2013) answers
;; this by stopping the system, reloading namespaces, and starting
;; a new one. DI offers a smaller-scope answer for the common case:
;; you don't have to restart anything to update a service.

;; ## Redefining a service

;; A service is bound as `(partial #'the-var deps)`. There is one level
;; of var indirection between the running system and the function
;; body. When you redefine the var with `defn`, the next call goes
;; through the new function immediately — no restart needed.

(t/deftest redefine-service-test
  (defn f [{x ::x} arg]
    [::f x arg])
  (with-open [root (di/start `f {::x :x})]
    (t/is (= [::f :x 42] (root 42)))
    ;; redefine f while the system is running
    (defn f [deps arg]
      [::new-f (deps ::x) arg])
    ;; the running root picks up the new implementation
    (t/is (= [::new-f :x 42] (root 42)))))

;; Notice the new implementation received the same dependency map
;; (`{::x :x}`) the system was started with. DI does not recompute
;; the wiring on redef.

;; ## When a restart is needed

;; The var indirection only covers the function body. The
;; dependency graph itself was decided at start. If you change
;; the declaration of the first argument — the dependency map —
;; the running system keeps the old wiring. Stop and start again
;; to apply the change.

;; The same applies to components — their built value is what the
;; system holds. Redefining the var does not rebuild the component.

;; The next chapter zooms in on the registry: how to override
;; dependencies, stack overrides, and what last-wins means.
