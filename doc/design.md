# Design

DI is built on one idea: a **registry middleware** — a plain function
that takes one registry and returns another. Resolving a symbol to its
var, overriding a key for a test, adding logging — each is a middleware
that reshapes how keys resolve. Learn that one shape, and the rest of
the library follows.

Before middleware, there are two smaller terms.

A **factory** is what a key resolves to — it makes one object and
declares its dependencies.

A **registry** is a function from a key to a factory.

A middleware then transforms one registry into another. That is the
unit you write and compose, and `di/start` folds your middlewares over
the default registry.

What all this gives you is **dependency wiring**. You write
components and declare what each one depends on. DI works out the order
and builds every component the root needs — only those, lazily —
passing each the dependencies it asked for. Nothing is wired by hand.
The wiring is computed once, when
[`di/start`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#start)
runs, and what you get back is the running system: the object built for
the root.

## Keys

A key names a component of the system. It is one of three things, and
the kind signals intent:

- a **symbol** points at a var, e.g. `` `my.app/server ``;
- a **keyword** names an abstraction, e.g. `:my.app/clock`;
- a **string** names an environment variable, e.g. `"PORT"`.

These are conventions, not hard rules, and a middleware can add more.
`di/env-parsing`, for example, reads a qualified keyword like
`:env.long/PORT`: the namespace selects a parser and the name is the
environment variable.

## The factory

A factory is what a key resolves to. It knows three things — what it
depends on, how to build the object, and how to describe itself — one
method each on the
[`Factory`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.protocols#Factory)
protocol:

```clojure
(defprotocol Factory
  (dependencies [this])
  (build        [this deps add-stop])
  (description  [this]))
```

- **`dependencies`** returns the keys the factory needs, each marked
  `:required` or `:optional`. A map will do. When build order matters —
  as it does for side effects like migrations — return an ordered
  sequence of `[key dep-type]` pairs instead: keys are built in the
  order they are listed. It may compute the answer, but it must be pure
  and stable, because DI asks more than once per `di/start`.
- **`build`** takes the resolved dependencies and returns the object.
  Its `add-stop` argument registers cleanup. See
  [Building and stopping](#building-and-stopping).
- **`description`** returns a diagnostic map and carries no behaviour.
  Inspection and logging read it.

### Most things are already factories

You almost never need to implement the protocol yourself. Most things
you write are factories already:

- a **`defn`** becomes a factory when DI resolves its symbol —
  [a component or a service](/doc/tutorial/a_your_first_system_test.md);
- [`di/ref`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#ref),
  [`di/template`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#template)
  and
  [`di/derive`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#derive)
  return factories;
- **any plain value is a factory** — it builds to itself.

That last rule is why a map of overrides works. When you start a system
with `{:my.app/clock a-fixed-clock}`, the value `a-fixed-clock` is used
as the factory for `:my.app/clock`: a factory with no dependencies that
builds to itself.

You reach for the protocol directly only when a key needs resolution
those can't express — most often, to wrap another factory.

### Building and stopping

`build` returns the object. Its third argument, `add-stop`, registers a
zero-arg cleanup procedure — call it once per resource you allocate.
Registered cleanups run in LIFO order when the system stops, so a
component is torn down before the dependencies it was built from. See
[Stopping components](/doc/tutorial/c_stopping_components_test.md).

`add-stop` is what lets factories wrap cleanly. Earlier versions handled
cleanup through `demolish` — a separate `Factory` method DI called with
the built object at stop time. That made wrapping leak. A factory that
wrapped another and allocated a resource of its own had to either return
an object proxying the inner one's teardown, or take over the inner
factory's cleanup itself. Either way it had to know how the inner
factory was torn down. With `add-stop` each factory registers its own
cleanup the moment it allocates — the inner one included. Wrapping no
longer touches teardown:

```clojure
(reify p/Factory
  (dependencies [_] (p/dependencies inner))
  (build [_ deps add-stop]
    (let [obj      (p/build inner deps add-stop)  ; inner registers its own stop
          resource (allocate-something obj)]
      (add-stop #(release resource))
      (wrap obj resource)))
  (description [_] (p/description inner)))
```

### The two meanings of nil

`nil` means *nothing here*, and DI leans on that in one place after
another. The bare registry underneath everything resolves every key to
`nil`. An `:optional` dependency that resolves to `nil` is skipped. A
`:required` one that resolves to `nil` fails with `::missing-dependency`.
The rule is uniform: a `build` that returns `nil` produced nothing.

That leaves no way to say *the value is `nil`*. Picture a component that
creates a table:

```clojure
(defn create-users-table
  {::di/kind :component}
  [{db `db}]
  (ddl db "create table users ...")
  ;; added during a later refactor
  (when remove-legacy?
    (ddl db "drop table legacy_users ...")))
```

When `remove-legacy?` is false the `when` returns `nil`, so the
component returns `nil`. Without help the system would fail to start
with `::missing-dependency` — and only in the configuration where the
branch is not taken, so tests pass and production breaks.

So a `:component` that returns `nil` is stored as the sentinel
`::di/nil`. The key counts as built, satisfies a `:required` dependency,
and dependents receive `::di/nil`:

```clojure
(with-open [system (di/start `create-users-table)]
  @system)  ; => :darkleaf.di.core/nil
```

The substitution is specific to `:component` factories. Every other
factory returns its value unchanged, so a custom factory that builds
`nil` is read as *nothing*. Reach for `di/opt-ref` or an `:optional`
dependency when *absent* is what you actually mean.

### Wrapping a factory

The protocol's shape is what lets one factory wrap another — the
decorator pattern. A wrapper declares the inner factory's dependencies,
adds any of its own, delegates `build` to the inner one, and layers
behaviour around the result. Here is a wrapper that logs
when its key is built. It adds a dependency on a `logger` and otherwise
stays out of the way:

```clojure
(defn with-logging [inner]
  (reify p/Factory
    (dependencies [_]
      (concat (p/dependencies inner) {`logger :required}))  ; inner deps plus a logger — concat, since deps may be a sequence
    (build [_ deps add-stop]
      (let [logger (deps `logger)
            obj    (p/build inner deps add-stop)]         ; inner builds, registers its own stop
        (logger {:built obj})
        obj))
    (description [_]
      (p/description inner))))
```

This wrap-and-delegate pattern is what the library is built on. The next
sections show a registry handing out factories, then a middleware
applying such a wrap across the whole registry.

## The registry

A registry is a function from a key to a factory:

```clojure
(fn [key] factory)
```

A function with this shape is the most general way to express a lookup,
so nothing else is needed. Even a map lookup is a function from key to
value.

A registry hands back a factory, not a built object. Nothing is built
when you look a key up. Building happens later, when `di/start` resolves
the root, reads each factory's dependencies, and builds them in order.

Only one registry is ever written out: a bare one that resolves every
key to `nil`. Every other registry is a closure that a middleware
returns.

## Middleware

A middleware takes a registry and returns a new registry. If you have
written Ring middleware, the shape is familiar: a function that wraps a
handler and returns a new one, with a registry in place of the handler.
Naming the two functions makes it clear:

```clojure
(fn middleware [registry]
  (fn new-registry [key]
    (registry key)))
```

That body, `(registry key)`, is a plain passthrough: the new registry
resolves every key exactly as the old one did. Every middleware starts
there. It becomes useful by returning a different factory for some keys,
or by wrapping the factory the inner registry returns.

`with-clock` does the first — it returns a fresh factory for `::clock`
and passes the rest through:

```clojure
(defn with-clock []
  (fn middleware [registry]
    (fn new-registry [key]
      (if (= key ::clock)
        (reify p/Factory
          (dependencies [_] nil)
          (build [_ _ _] (Clock/systemUTC))
          (description [_] {}))
        (registry key)))))
```

`with-logging` above does the second — it wraps the factory the inner
registry returns. This shape repeats across `core`: `di/update-key`,
`di/log`, `di/add-side-dependency` and the rest each look up a factory,
override one method, and delegate the others.

The built-in middlewares wrap the bare registry — one resolves a symbol
to its var, another resolves a string to an environment variable. That
stack is the default registry, no different from the middlewares you
write. `di/start` folds your middlewares over it, each wrapping the one
before — so the last one you pass is the outermost, and a key reaches it
first. For convenience, `di/start` also accepts a map of overrides,
`nil` for a no-op, or a sequence of these, and normalizes each into the
same registry transformation — see
[The middleware argument](/doc/reference/middleware_argument.md) for the
full list.

## Inspection: walking the graph without building it

DI infers the wiring for you. You never write out the dependency graph —
each factory declares its own `dependencies`, and `di/start` works out
what to build and in what order. That is convenient, but implicit: from
the code alone, the shape of the running system is not obvious.

`di/inspect` makes it explicit, and it builds nothing. You call it like
`di/start` — the same key and middlewares — but it returns the graph
instead of building the system. It is not a separate mechanism: it adds
a middleware of its own that wraps every factory with another whose
`build` records the key, its dependencies, and its description instead
of constructing the object:

```clojure
[{:key `root :dependencies {`foo :required `bar :optional}}
 {:key `foo}
 {:key `bar}]
```

The graph is walked exactly as `di/start` would walk it, so the report
reflects the system you would actually get — middlewares applied,
overrides in place. The only difference is that `build` describes
instead of constructs: no objects are built, no side effects run.

Inspection costs nothing extra because `description` is part of the
`Factory` protocol. However deeply a factory is wrapped, it can still
describe itself, so the whole system stays readable after every
middleware is applied.

## di/start: building and the running system

`di/start` folds the middlewares over the default registry, then
resolves the root, then its dependencies, then theirs. That is a pull:
a component is built only because something above it asked for it. A
factory in the registry that the root never reaches is never built, and
an `:optional` dependency that resolves to nothing is simply skipped.

What you get back is the object built for the root, wrapped in a
container. Two interfaces matter most:

- **`IDeref`** — deref it to get the built object.
- **`AutoCloseable`** — closing it stops the whole system.

The rest is convenience, so you can reach the object without a deref
first:

- **`IFn`** — if the root is a function, call the container directly. It
  delegates across every arity.
- **`Indexed`** and **`ILookup`** — destructure it as a vector or a map,
  matching a vector or map of root keys (see
  [Starting many keys](/doc/tutorial/h_starting_many_keys_test.md)).

Pulling from the root is what makes feature flags cheap. A subsystem
adds itself to the registry through a middleware (see
[Composition with update-key](/doc/tutorial/k_composition_with_update_key_test.md)).
Put that behind a flag, and when the flag is off the subsystem
contributes nothing — nothing references its keys, so they are never
built. One binary ships everywhere, each environment building only the
features it turns on. See
[Why DI?](/doc/why_di.md#feature-flags-come-almost-for-free) for the
payoff in full.


## The whole model

Step back, and the model is small. A **factory** makes one object and
declares its dependencies. A **registry** is a function from a key to a
factory. A **middleware** is a function from a registry to a registry.
`di/start` folds the middlewares over the default registry, pulls the
root, and hands back the running system.

The engine at the centre is deliberately small. Almost every feature is
implemented as middleware layered on top of it — symbol resolution,
environment variables, overrides, logging, inspection, memoization, and
the rest.

Factories and registry middleware have been enough for the whole
library since the first release. Only the `Factory` protocol was refined over time to improve
composability. The model is stable now, and
there are no plans to change it.
