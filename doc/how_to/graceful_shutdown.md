**DRAFT**

# Graceful shutdown

In production, the process must stop cleanly. An orchestrator —
systemd, Kubernetes, Docker — stops a service by sending the
`SIGTERM` signal, and the app gets a short period to release what
it holds: stop accepting requests, finish the requests in flight,
close the connection pool.

This how-to shows a `-main` that starts a DI system and stops it
on shutdown.

## The recipe

The JVM runs *shutdown hooks* on `SIGTERM`, on `Ctrl-C`
(`SIGINT`), and on a normal `System/exit`. Register a hook that
stops the system:

```clojure
(ns app.main
  (:require
   [app.system :as system]
   [darkleaf.di.core :as di])
  (:gen-class))

(defn -main [& _args]
  (let [root (di/start `system/root (system/registry))]
    (.. Runtime
        getRuntime
        (addShutdownHook (Thread. #(di/stop root) "di-shutdown")))))
```

That is the whole recipe. Two details make it work:

- **Nothing blocks explicitly.** After `-main` returns, the JVM
  keeps running as long as some component owns a non-daemon
  thread — a Jetty server, a worker pool. When the process
  receives a signal, the JVM runs the hooks and exits.
- **Stop order is the reverse of build order.** The server was
  built after the connection pool it depends on, so it is stopped
  first: the system stops accepting requests before the pool they
  use is closed.

A start failure needs no hook. If `di/start` throws, it has
already stopped the components it managed to build (see
[Handling start failures](/doc/tutorial/l_handling_start_failures_test.md)),
the exception reaches the top of `-main`, and the JVM exits with
a stack trace and a non-zero code. The hook is registered only
after a successful start, so nothing is stopped twice.

## What the hook does not cover

- `SIGKILL` and JVM crashes skip shutdown hooks. Orchestrators
  send `SIGKILL` when the grace period runs out — for example,
  Kubernetes waits 30 seconds by default. Keep your stop
  functions fast, or raise the grace period.
- Hooks run concurrently with the rest of the JVM. Do not call
  `System/exit` from inside a hook.

The JDK also has `sun.misc.Signal` for handling arbitrary
signals, but it is an internal API and may change between JDK
releases. Shutdown hooks are the supported mechanism, and they
cover `SIGTERM` and `SIGINT` — enough for a typical service.
