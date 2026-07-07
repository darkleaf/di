# Why DI?

A typical Clojure project has a few stateful objects — a database
pool, an HTTP server, maybe a cache or a worker queue — surrounded
by many stateless functions. DI starts from the idea that all of
them are first-class components: the stateful objects and the
stateless functions alike. Declaring "this function needs the
datasource" should cost no more than writing a `defn`.

Everything else follows from that.

## Dependencies live with the function that uses them

Each function lists its dependencies in its own argument list:

```clojure
(defn show-user [{ds ::db/datasource} req]
  (jdbc/get-by-id ds (-> req :path-params :id)))
```

There is no central wiring file. DI inspects the keys you
destructure and resolves them during start.

## Lazy initialization

DI builds only what your root needs, directly or through other
dependencies. A component that nothing uses is never started.

## Configuration is just components

Environment variables are the default — a string key in your deps
resolves to an env var. This is plain
[12-factor](https://12factor.net/config) style: a flat namespace of
names and string values, no nested config shape to design or
maintain. If you prefer a config file, read EDN into a map and pass
that map to DI when you start the system. Validation is just a
component that checks its own dependencies — bad configuration
is caught at start, not in production. The recipe:
[Startup checks](/doc/how_to/startup_checks_test.md).

For typed values, qualified keys like `:env.long/PORT` or
`:env.json/SETTINGS` parse the env var on the way in. The keyword
namespaces (`:env.long`, `:env.json`, anything you like) are yours
to define.

## Subsystems compose without referencing each other

A subsystem owns its handlers and adds them to a shared registry.
The namespace that owns the registry does not know about the
subsystem. Adding a subsystem means adding a file — no edits to
existing ones.

Concretely, the users subsystem ships a `registry` function that
hooks itself onto the central routes:

```clojure
;; app/users.clj
(defn registry [-feature-flags]
  [(di/update-key `app.web/routes conj (di/ref `users-routes))])
```

The main system composes registries from every subsystem. `app.web`
never imports `app.users`. A third subsystem is a third file with
its own `registry`. The pattern is explained in
[Composition with `update-key`](/doc/tutorial/k_composition_with_update_key_test.md).

## Feature flags come almost for free

Combine lazy initialization with subsystems that own their wiring,
and feature flags fall out naturally. Flip a flag, and the
subsystem contributes nothing to the registry — the components
behind it stop being built. One binary ships to many environments,
each with a different set of features active. The recipe:
[Feature flags](/doc/how_to/feature_flags_test.md).

## Wiring inside data

When you describe something in data — reitit routes, scheduler
tables, connection-pool configs — you can put `(di/ref ...)` right
inside the data:

```clojure
(def route-data
  (di/template
   [["/users/:id" {:get {:handler (di/ref `show-user)}}]]))
```

`di/template` walks the structure on start and replaces each ref
with the built component.

## Cross-cutting changes don't touch the original code

Wrap a function with metrics, schema validation, or any decorator
— without editing the function itself. The wrapping lives in the
registry. See
[`di/update-key`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#update-key).

## Two kinds of binding

A stateful component arrives in the function as the built value
itself — no atom to deref, no runtime lookup. To swap one, you
change the registry and restart.

A stateless service is bound as `(partial #'the-var)`. There is one
level of var indirection, by design: it is exactly what makes live
REPL redefinition work (next section). The component/service
distinction is defined in
[Your first system](/doc/tutorial/a_your_first_system_test.md).

## The system is a value

`di/start` returns a system root, not a global singleton. Deref it
to get the built component. You can hold a reference to it, start a
second system alongside, pass it around, and stop it independently.
No global registry to reset, no namespace to reload.


## Live redefinition works

Redefine a service with `defn`, and the running system uses the
new version immediately. No restart, no lost state. But the
service keeps receiving the dependencies that were resolved at
start — to add a new dependency, restart the system. The
workflow:
[Interactive development](/doc/tutorial/d_interactive_development_test.md).

## Tests share a cached system

Each test starts its own system and stops it normally. Components
built in one test are reused by the next — DI caches them across
tests via
[`di/->memoize`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#->memoize).
The whole suite runs as fast as a single system start. Teardown
happens once, at the end. The recipe:
[Reusing components between tests](/doc/how_to/reusing_components_between_tests_test.md).

## Partial start failures are contained

If start fails halfway through, DI stops the components it already
started before propagating the error. If those stops also fail,
their exceptions are captured alongside the original — nothing is
lost.

---

The [tutorial](/doc/tutorial/a_your_first_system_test.md) walks through each of
these, one chapter at a time. For the model behind the library,
read [Design](/doc/design.md); if you are choosing between DI and
Integrant, see [Integrant vs DI](/doc/integrant.md).
