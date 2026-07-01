# The middleware argument

`di/start`, `di/inspect` and `di/->memoize` all take a variadic
`middlewares` argument. A middleware wraps a registry to produce a
new one — see
[Design](/doc/design.md#middleware) for the
concept. This page describes the values the argument accepts: a
function, a map, a sequence, `nil`, or a
`java.util.function.Function`.

## Function

A function `registry -> key -> Factory`. The most general, and the
one the others reduce to. The inner function is the new registry,
so it either delegates to the one beneath it or answers with a
factory of its own:

```clojure
;; pass every key through to the registry beneath
(fn middleware [registry]
  (fn new-registry [key]
    (registry key)))

;; or answer with a factory of your own
(fn middleware [registry]
  (fn new-registry [key]
    (reify p/Factory
      ...)))
```

Every built-in (`di/update-key`, `di/env-parsing`, `di/log`,
`di/ns-publics`, …) is a function of this shape.

## Map

A map of `{key factory}`. It overrides the listed keys and
delegates the rest to the registry beneath it:

```clojure
(di/start `root {`db         test-db          ; a plain object, built as-is
                 ::clock     (di/ref `fixed)  ; a built-in Factory
                 `cache      (reify p/Factory ; a hand-written Factory
                               ...)
                 "LOG_LEVEL" "debug"})        ; a plain value too
```

A value is used as the factory directly. A plain object counts as
a factory that builds to itself, so you can drop in a stub or a
literal without wrapping it; a value like `(di/ref ...)` or a
`reify p/Factory` is a factory in its own right. This is the most
common way to override components in tests and configuration.

## Sequence

A vector or seq of the other values, applied left to right and
flattened into the chain. It pairs naturally with `nil`: build the
sequence from `when` expressions, and the ones whose `when` is false
become `nil`, which is a no-op.

```clojure
(defn dev-middlewares [{:keys [stub-db? verbose?]}]
  [(when stub-db? {`db test-db})
   (when verbose? (di/log :after-build! report))])

(di/start `root (dev-middlewares flags) {::override :x})
```

A sequence may contain any of these values, including other
sequences.

## nil

A no-op. Handy for a middleware that is only sometimes present,
without an `if` around the whole call:

```clojure
(di/start `root (when dev? dev-middlewares))
```

## java.util.function.Function

A `java.util.function.Function` from registry to registry — the
same mapping as the function above, called through `.apply`.

This exists for a stateful middleware that has to be more than
a function. `di/->memoize` returns a value that is both a `Function`
(so it works as a middleware) and `AutoCloseable` (so `di/stop` can
release everything it cached). A plain Clojure `fn` can't carry a
second interface like that, so the registry accepts a `Function`
object as an alternative.

## Order

Two directions are in play, and they are opposite.

The values you pass are *applied* left to right: the leftmost wraps
the default registry, the next wraps that, and so on, so the
rightmost ends up outermost. A *lookup* then runs through that stack
from the outside in, so the rightmost middleware sees each key
first. When two of them answer the same key, the rightmost therefore
takes effect:

```clojure
(di/start `root {`x :a} {`x :b})   ; `x resolves to :b
```

Keep in mind what a middleware actually does. A middleware only
chooses, per key, whether to answer or to delegate to the registry
beneath it.

`di/->memoize` is constrained the other way: it must be the
*first* (leftmost) middleware. It does not wrap the registry it is
handed. It carries its own, built from the middleware passed to
`->memoize`, and rejects anything applied before it with
`::wrong-memoized-registry-position`. So anything that would sit
beneath `mem` has to be passed into it instead, and overrides go
after it, on the outside:

```clojure
;; wrong — a middleware before mem is rejected
(di/start `root base-middlewares mem)        ; throws

;; right — fold them into mem, keep overrides on the outside
(def mem (di/->memoize base-middlewares))
(di/start `root mem {::override :x})          ; ok
```

## Why a `cond`, not a protocol

`di/start` dispatches over these values with a `cond` over standard
predicates, not a protocol. A protocol does not fit. You would have
to extend it onto maps, sequences and functions — abstract
groupings, not single types. A sequence alone covers lists,
vectors, lazy seqs, cons cells, and more. And the cases need an
explicit precedence that type-based dispatch does not give: a map
is also a collection, so it has to be recognised as a map of
overrides before the general sequence case. A `cond` over `map?`,
`sequential?`, `fn?` and `instance? Function` states both the
grouping and the order in one place.

Because a function is already accepted, it doubles as the
extension point: to plug in a type of your own, convert it yourself
and pass the result, which is then an ordinary function or
`Function`.

```clojure
(di/start `root (your-thing->middleware x))
```
