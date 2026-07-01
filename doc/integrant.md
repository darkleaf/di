# Integrant vs DI

*A comparison of two dependency injection frameworks for Clojure: weavejester's Integrant and darkleaf's DI.*

DI was built for a specific shape of system: a handful of stateful
objects (a database pool, an HTTP server, a queue) surrounded by many
stateless functions. Declaring a stateless function as a component
should cost no more than writing a `defn`. This constraint drives
the differences between [DI](https://github.com/darkleaf/di)
and [Integrant](https://github.com/weavejester/integrant) — how each
handles dependencies, REPL workflow, composition, and failure.

```clojure
(ns integrant
  (:require
   [integrant.core   :as ig]
   [darkleaf.di.core :as di]
   [app.jetty        :as-alias jetty]
   [app.web          :as-alias web]
   [app.db           :as-alias db]
   [app.subsystem-a  :as-alias subsystem-a]
   [app.subsystem-b  :as-alias subsystem-b]))
```

## Assumptions

- All snippets share the namespace shown above.
- Library calls (Jetty, Reitit) are stubbed. The [example app](/doc/example.md) has runnable wiring.

## Code Examples

A throwaway `stop` placeholder used in the examples below:

```clojure
(defn stop
  "Fake generic stop function."
  [x])
```

### Jetty Server

#### Using Integrant

```clojure
(defmethod ig/init-key ::jetty/server [_ {:keys [handler port]}]
  ;; In a real application, you might use:
  ;; (jetty/run-jetty handler {:port port, :join? false})
  [:jetty handler port])

(defmethod ig/halt-key! ::jetty/server [_ server]
  (stop server))
```

#### Using DI

```clojure
(defn jetty-server
  {::di/stop #(stop %)}
  [{handler ::jetty/handler
    port    "PORT"}]
  [:jetty handler port])
```

With DI, the destructuring map uses three kinds of keys:

- **Symbols** (e.g. `` `handler ``) refer directly to a Clojure var.
- **Keywords** (e.g. `::jetty/handler`) are abstract — the registry
  binds them to a concrete implementation at start time.
- **Strings** (e.g. `"PORT"`) read from the environment.

### Routes

#### Using Integrant

```clojure
(defmethod ig/init-key ::web/route-data [_ {:keys [root-handler]}]
  [["/" {:get {:handler root-handler}}]])
```

#### Using DI

```clojure
(def route-data
  (di/template
   [["/" {:get {:handler (di/ref `root-handler)}}]]))
```

``(di/ref `root-handler)`` resolves to the `root-handler` var —
no need to register `root-handler` as a separate component and
thread it through the config map the way Integrant requires.

### Reitit

Both frameworks transform `route-data` into a Ring handler via a plain function:

```clojure
(defn route-data->handler [route-data]
  ;; In a real application, you might use:
  ;; (-> route-data
  ;;     (ring/router)
  ;;     (ring/ring-handler))
  [:ring-handler route-data])
```

#### Using Integrant

```clojure
(defmethod ig/init-key ::web/handler [_ {:keys [route-data]}]
  (route-data->handler route-data))
```

#### Using DI

When the component is one dep passed through a function, `di/derive`
is the most concise form:

```clojure
(def web-handler (di/derive `route-data route-data->handler))
```

For anything more involved, declare deps via destructuring in a
normal `defn`:

```clojure
(defn web-handler [{route-data `route-data}]
  (route-data->handler route-data))
```

### Handlers

#### Using Integrant

```clojure
(defmethod ig/init-key ::web/root-handler [_ {:keys []}]
  (fn [req]
    :ok))
```

#### Using DI

```clojure
(defn root-handler [-deps req]
  :ok)
```

With DI, a service like `root-handler` resolves through its var at
call time. Re-evaluating the `defn` form updates the running system
without a restart — the next request goes through the new
implementation, reusing the already-built dependencies. Adding a
*new* dependency to the function still requires a restart — the
dependency graph is fixed at start time.

In Integrant, `init-key` runs once and closes over the deps it
received, so the running handler keeps pointing at the old function.
Integrant codebases sidestep this by keeping handlers out
of the system and passing deps through the Ring request via
middleware — see "Real Applications" below. Integrant's
`suspend-key!` and `resume-key` solve a different problem:
preserving long-lived resources like open sockets across a rebuild.

### System Initialization

#### Using Integrant

```clojure
(def ig-config
  {::jetty/server     {:port    8080
                      :handler (ig/ref ::web/handler)}
   ::web/route-data   {:root-handler (ig/ref ::web/root-handler)}
   ::web/handler      {:route-data (ig/ref ::web/route-data)}
   ::web/root-handler {}})

(ig/init ig-config)
```

#### Using DI

```clojure
(di/start `jetty-server {"PORT"         8080
                         ::jetty/handler (di/ref `web-handler)})
```

The Integrant config enumerates every key and the references between
them. The DI call lists only what isn't derivable from the code: the
env var `"PORT"`, and the binding for the abstract `::jetty/handler`
key. The rest lives in function signatures, where DI reads it:
`jetty-server` depends on `::jetty/handler`, `web-handler` depends on
`route-data`, and so on.

`::jetty/handler` is an abstraction: the Jetty namespace declares the
dep without naming the reitit handler that fulfils it. The registry
binds the abstract key to a concrete implementation, or to a mock
for tests.

If you do want the topology as data, `di/inspect` takes the same
arguments as `di/start` and returns the resolved graph as a vector of
`{:key … :dependencies … :description …}` maps without running
anything. Feed it to a graph visualiser, diff between deployments, or
assert a structure in tests — see the [Inspect reference](/doc/reference/inspect_test.md).


## Real Applications

In any real application, stateless functions depend on stateful
components: a handler queries a database via a datasource, an
auth check decodes a token with a secret key. The
question is how the handler gets to the datasource.

Integrant (and Stuart Sierra's
[Component](https://github.com/stuartsierra/component)) give you two
options:

1. **Make every stateless function a component.** Each handler
   becomes a multimethod with its own config entry. The system map
   grows roughly linearly with the number of handlers.
2. **Pass the system map through the Ring request via middleware**
   ([example](https://github.com/prestancedesign/usermanager-reitit-example/blob/main/src/usermanager/controllers/user.clj#L73)).
   Handlers stay simple, but each one reaches into `req` for
   dependencies that aren't visible in the function signature.

DI offers a third option: declare deps in the function signature,
the same way Clojure namespaces declare `:require` blocks.

```clojure
(defn root-handler* [{db      ::db/datasource
                      decoder `token-decoder}
                     req]
  ...)

(defn token-decoder [{key "SECRET_KEY"} token]
  ...)
```

`::db/datasource` is an abstract dependency bound in the registry,
`` `token-decoder `` is a Clojure var, and `"SECRET_KEY"` is an
environment variable. Adding a new stateless function costs one
`defn` — no key registration, no multimethod, no config entry.

## Subsystems

A web application often groups independent subsystems behind shared
plumbing — one Jetty server, one router, separate route trees. With
Integrant, the routing namespace must list every subsystem in its
`init-key`: `::web/route-data` references `::subsystem-a/route-data`,
`::subsystem-b/route-data`, and so on. The list grows every time you
add a subsystem, and the route-data key has to know about all of
them.

With DI, each subsystem extends the shared key independently. The
routing namespace never names any subsystem:

```clojure
(def subsystem-a-route-data
  [["/subsystem-a/" '...]])

(defn subsystem-a []
  (di/update-key `route-data concat (di/ref `subsystem-a-route-data)))
```

```clojure
(di/start `jetty-server
          {"PORT"         8080
           ::jetty/handler (di/ref `web-handler)}
          (subsystem-a)
          (subsystem-b)
          #_(subsystem-N))
```

Adding `subsystem-N` takes one line in the registry. The routing
namespace stays untouched.

## Feature Flags

Feature flags are usually preferable to `(if env-X?)` checks scattered
through business code. Each conditional blurs the business logic —
reading the function means following both branches at once. Lifting
the flag out keeps each function linear and single-purpose.

In DI, flags are an argument to the registry-building function. Each
subsystem's contribution is a closure over that flags map, and DI
skips `nil` middlewares — so a disabled subsystem returns `nil` and
disappears from the registry:

```clojure
(defn subsystem-a* [{:keys [subsystem-a-enabled]}]
  (when subsystem-a-enabled
    (di/update-key `route-data concat (di/ref `subsystem-a-route-data))))

(defn registry [flags]
  [{"PORT"         8080
    ::jetty/handler (di/ref `web-handler)}
   (subsystem-a* flags)
   #_(subsystem-N flags)])

(di/start `jetty-server (registry {:subsystem-a-enabled true}))
```

The registry function is the single place where deployment decisions
live. Turning a flag off means deleting one line. The subsystem's own
namespace stays untouched. Tests pass whatever flags map they want —
no env-var mocking, no global state.

## Error Handling on Failed Start

When a component throws during build, DI stops every component
already built and rethrows a build-failure exception with the
original as its cause. Each stop runs independently — exceptions
raised during cleanup are attached to the thrown exception as JVM
[suppressed exceptions](https://docs.oracle.com/javase/8/docs/api/java/lang/Throwable.html#getSuppressed--),
so one failed start surfaces the root cause and every cleanup failure
in a single stack.

Integrant works differently. `ig/init` throws an `ExceptionInfo` whose
`ex-data` carries the partially built system under `:system`. You (or
[integrant-repl](https://github.com/weavejester/integrant-repl)) call
`ig/halt!` on it to roll back. But `ig/halt!` itself aborts on the
first `halt-key!` that throws — every component scheduled to halt
after the failing one leaks.

[Stuart Sierra's Component](https://github.com/stuartsierra/component)
behaves the same way as Integrant here. `stop-system` calls each
component's `stop` via `reduce`. The first throwing `stop` aborts the
rollback, leaving the partially stopped system in `ex-data` under
`:system` for the caller to handle.

## Aspect-Oriented Programming (AOP)

It's common to add cross-cutting behaviour to a component without
modifying it — logging, tracing, error reporting, schema validation.

In Integrant, there is no mechanism to wrap an existing component
from the outside. The options are: edit the component's `init-key`
to add the wrapper inline (couples the wrapper to the owning
namespace), or rename the key and re-point every dependent
(see [weavejester/integrant#58](https://github.com/weavejester/integrant/issues/58)).
Neither scales when the wrapping is optional or originates outside
the component's namespace.

`di/update-key` provides that mechanism. A real example: adding New
Relic instrumentation to a running system. The instrumentation module
is a single namespace, none of the wrapped components import it, and
removing the module from the registry removes all instrumentation:

```clojure
(ns app.decorators.newrelic
  (:require
   [darkleaf.di.core :as di]
   [app.jetty        :as-alias jetty]))

(defn- wrap-notice-error   [handler] ...)
(defn- wrap-tx-name        [handler] ...)
(defn- wrap-custom-attrs   [handler] ...)
(defn- wrap-template-name  [handler] ...)

(defn registry [{:keys [wrap-notice-error?]}]
  [(when wrap-notice-error?
     (di/update-key ::jetty/handler    wrap-notice-error))
   (di/update-key `app.api/middleware  #(into [wrap-tx-name] %))
   (di/update-key `app.auth/wrap-auth  comp wrap-custom-attrs)
   (di/update-key `reitit/middleware   conj wrap-template-name)])
```

Four components in three different namespaces wrapped from one place.
None of them imports the New Relic module. The same pattern in
Integrant requires editing each of the four owning namespaces, or
introducing four parent keys via `derive` and reconfiguring every
dependent.

A caveat: every `update-key` adds behaviour invisible from the
wrapped component's source. Use it for cross-cutting concerns —
instrumentation, feature flags, test wrappers — not as a
shortcut around editing a component you own.

---

DI components are plain Clojure functions. Each declares its direct
dependencies in its argument destructuring, and the full system graph
emerges from those declarations. Namespaces extend each other's keys
with `di/update-key` and `di/add-side-dependency`. Neither namespace
needs to know about the other. The result is shorter configuration,
hot reload of stateless code without restart, and cross-namespace
composition without editing the code being composed.

This pattern has been running in production for several years in a
large Clojure backend with eight loosely-coupled subsystems that each
contribute routes, middlewares, and side dependencies to shared keys
without the owning namespaces ever importing them.

If you want the dependency graph centralised in an EDN
file — a single map you can read top-to-bottom to see how the system
fits together — Integrant fits that workflow better.
