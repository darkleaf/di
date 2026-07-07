# Graceful shutdown

In production, the process must stop cleanly. An orchestrator —
systemd, Kubernetes, Docker — stops a service by sending the
`SIGTERM` signal, and the app gets a short period to release its
resources: stop accepting requests, finish the requests it has
already accepted, close the connection pool.

`di/stop` performs this kind of stop. It stops components in
the reverse order of the build order: the server was built after
the connection pool it depends on, so the server is stopped
first — the system stops accepting requests before the pool those
requests use is closed.

`di/stop` has to be called when the process receives the signal.
Different teams solve this in different ways; both of the
following approaches are used in production by projects built on
DI.

In both examples, the shutdown code is registered only after a
successful `di/start`, so nothing is stopped twice. A start
failure needs no extra code: if `di/start` throws, it has already
stopped the components it managed to build (see
[Handling start failures](/doc/tutorial/l_handling_start_failures_test.md)),
and the JVM exits with a stack trace and a non-zero code.

## A JVM shutdown hook

The JVM runs *shutdown hooks* on
`SIGTERM`, on `Ctrl-C` (`SIGINT`), and on a normal `System/exit`:

```clojure
(ns app.main
  (:gen-class)
  (:require
   [app.system :as system]
   [darkleaf.di.core :as di]))

(defn -main [& _args]
  (let [root (di/start ::system/root (system/production-registry))]
    (.. Runtime
        getRuntime
        (addShutdownHook (Thread. #(di/stop root))))))
```

## A signal handling library

[spootnik/signal](https://github.com/pyr/signal) installs a
handler for a named signal — here, for `SIGTERM`:

```clojure
(ns app.main
  (:gen-class)
  (:require
   [app.system :as system]
   [darkleaf.di.core :as di]
   [signal.handler :refer [with-handler]]))

(defn -main [& _args]
  (let [root (di/start ::system/root (system/production-registry))]
    (with-handler :term
      (di/stop root))))
```
