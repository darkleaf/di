# Instrumenting services

Say you want something to happen around every service call: an
APM span, a timing metric, an error report. The wrapper is the
same for every service. This page uses a NewRelic trace as the
example, but nothing below is specific to NewRelic, or even to
tracing.

Wrapping each service by hand spreads one concern over the whole
codebase, and the set of wrapped services drifts: services come
and go, and a new one reaches production with no instrumentation.
But every service is built in one place — the registry.
Instrument there, and no service can be missed.

## Services, not components

A component builds into a finished object; a service builds into
a function that is called at runtime
([Your first system](/doc/tutorial/a_your_first_system_test.md)).
A trace wraps a *call*, so the targets are services. And at build
time they are easy to tell apart: every factory describes itself,
and `(-> factory p/description ::di/kind)` answers `:service` or
`:component`.

## Why a new middleware

`di/update-key` decorates one named key. A registry map overrides
the keys it lists. Both need the list of services written by hand
— the same drifting list we are trying to avoid. What we want is
a decision made per key, for every key the system resolves: that
is the most general form of a middleware, a function `registry ->
key -> Factory`
([The middleware argument](/doc/reference/middleware_argument.md)).

## The middleware

```clojure
(ns app.instrument
  (:require
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

(defn instrument-service
  "f is (fn [service key] wrapped-service)"
  [f]
  (fn [registry]
    (fn [key]
      (let [factory (registry key)]
        (if (= :service (-> factory p/description ::di/kind))
          (reify p/Factory
            (dependencies [_]
              (p/dependencies factory))
            (build [_ deps add-stop]
              (-> (p/build factory deps add-stop)
                  (f key)))
            (description [_]
              (p/description factory)))
          factory)))))
```

For every key it asks the registry beneath for the factory and
checks its kind. Anything but a service passes through untouched.
A service gets a new factory in front of it, transparent in
everything except the built value:

- `dependencies` delegates unchanged — changing them would break
  the wiring;
- `description` delegates unchanged — `di/inspect`, `di/log` and
  other middlewares read it;
- `build` builds the real service, then hands it to `f` along
  with its key.

The wrapping happens inside `build` and not sooner, because the
service function does not exist until its dependencies are
resolved. And `f` receives the key because instrumentation needs
a name — the key *is* the name.

## The removed `di/instrument`

DI once had this idea in its general form: `di/instrument`, a
middleware that decorated every object the system builds. It was
removed ([Changelog](/doc/changelog)): a wrapper that applies to
everything must avoid instrumenting its own dependencies, must
respect stop procedures, and must make sense for objects it knows
nothing about — and no implementation handled all of that well.
Services are the special case where these problems disappear: a
service is a plain function with no stop procedure, and the
wrapper is passed as a plain value, not as a key to resolve. So
this recipe works where the general one did not.

## Applying it

`wrap-trace` here is the NewRelic wrapper; its definition is long
and mechanical, so it is moved to the end of the page:

```clojure
(declare wrap-trace)

(di/start :app.system/root
          (instrument-service
           (fn [service key]
             (wrap-trace service (str key)))))
```

Every service now reports a span named after its key, and a
service added later is covered automatically. To replace NewRelic
with OpenTelemetry, a timer, or a logger, change `f` — the
middleware stays the same.

## The wrapper

NewRelic's agent traces JVM methods annotated with `@Trace`. A
Clojure function cannot carry a JVM annotation, but a `reify`
method can — metadata on it compiles into a real annotation. So
the wrapper reifies `IFn`, annotates each method, and delegates.
Note the full method set: besides the twenty-one `invoke` arities
and `applyTo`, `IFn` extends `Callable` and `Runnable`, so `call`
and `run` must be wrapped too — an executor calls the service
through them, bypassing every `invoke`:

```clojure
(ns app.newrelic
  (:import
   (com.newrelic.api.agent NewRelic Trace)
   (clojure.lang Fn IFn)))

(defn- set-trace-name [name]
  (.. NewRelic getAgent getTransaction getTracedMethod
      (setMetricName (into-array String ["Instrument" name]))))

(defn wrap-trace [^IFn service name]
  (reify
    Fn
    IFn
    (^{Trace true} call [_]
      (set-trace-name name)
      (.call service))
    (^{Trace true} run [_]
      (set-trace-name name)
      (.run service))
    (^{Trace true} invoke [_]
      (set-trace-name name)
      (.invoke service))
    (^{Trace true} invoke [_ a1]
      (set-trace-name name)
      (.invoke service a1))
    (^{Trace true} invoke [_ a1 a2]
      (set-trace-name name)
      (.invoke service a1 a2))
    ;; ...and so on for the arities your services use,
    ;; up to the full twenty, plus:
    (^{Trace true} applyTo [_ args]
      (set-trace-name name)
      (.applyTo service args))))
```

This complexity comes from NewRelic, not from DI. An
OpenTelemetry span or a plain timer does not need annotations,
and its `wrap-trace` is an ordinary higher-order function.
