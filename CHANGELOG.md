# 3.6.0

+ Breaking: Clojure 1.12
+ Breaking: `p/Factory` protocol was changed
+ Breaking: the description of some factories was changed
+ Breaking: internal keys were redacted
+ Add `->memoize`

# 3.5.0

+ Remove `*next-id*`
+ Update clerk
+ Prepare for new feature `di/->memoize`

# 3.4.0

+ Add the description for envs (#42)
+ Fix description for undefined keys (#43)

# 3.3.0

+ Add FactoryDescription protocol (#33)
  This release's headline feature allows you to fully inspect your system as data.
  If your development process heavily depends on feature flags, it is necessary to test your system.
  This feature now makes it possible.
+ Add better component validation (#39)
+ Wrap exceptions during the build process (500179a)
+ Lint di.core/with-open as clojure.core/with-open (#34)

# 3.2.1

+ Fixed `di/add-side-dependency` bug (#32)

# 3.2.0

+ Improved `di/update-key`. It will throw an exception on a non-existent key (#23)
+ [Added inspect middleware](https://github.com/darkleaf/di/commit/073471c9b3afcbcf52223b4607b9d21aa3865e35)
+ [Added log middleware](https://github.com/darkleaf/di/commit/cd52e5089d791b06004c669e6092eb98b040ef66)
+ [Added `di/with-open`](https://github.com/darkleaf/di/commit/7633474f3a2ed31cd810f73b7e703f7212114dd5)

+ [Added `di/*next-id*` to use it instead of `gensym` ](https://github.com/darkleaf/di/commit/b8ec1887fa5cd669af7663d74e03b2cfd2f0c58c)


+ [Added print-method for services](https://github.com/darkleaf/di/commit/b2868860c9b2a8149903345aebbb404d817c7ce2)
+ Improved naming of generated keys in update-key

# 3.1.0

+ Internal refactoring: switched from recursive functions to loop/recur to minimize stack traces in exceptions.
+ Missing and circular dependency exceptions now include a stack of keys inside `ex-info` for easier debugging.
+ Improved key generation: better naming of generated keys in `update-key`.
+ Increased test coverage to ensure better reliability.
+ Updated comparison with Integrant.

# 3.0.0

## New features

* `di/ns-publics` registry middleware

## Breaking changes

### Explicit separation of components and services

Use `{::di/kind :component}` to mark a component.

A zero arity service are not a component of zero arity function now.


```clojure
;; v2
(defn schema-component
  [{ddl `clickhouse/ddl
    ttl "TTL_DAY"
    :or {ttl "30"}}]
  (ddl ...))

;; v3
(defn schema-component
  {::di/kind :component}
  [{ddl `clickhouse/ddl
    ttl "TTL_DAY"
    :or {ttl "30"}}]
  (ddl ...))
```

```clojure
;; v2
(defn my-service []
  (fn []
    ...))

;; v3
(defn my-service []
   ...)
```

### Explicit separation of keys in `di/update-key`

Explicitly use `(di/ref)` or other factory to refer to a component.


```clojure
;; v2
(di/update-key `reitit/route-data conj `raw-method/route-data)

;; v3
(di/update-key `reitit/route-data conj (di/ref `raw-method/route-data))
```

```clojure
;; v2
(di/update-key `raw-method/methods #(assoc %1 param-name %2) method)))

;; v3
(di/update-key `raw-method/methods assoc param-name (di/ref method))))
```

### `di/derive` instead of `di/fmap`

The `di/fmap` factory constructor was removed.

Use `di/derive` instead.

### `di/instument` removing

The `di/instument` registry middleware was removed. Maybe there will be a rewrited version.

# 2.4.2

## Fixing `di/add-side-dependency`

# 2.4.1

## Fixing `di/add-side-dependency`

When root had eight dependencies a system with side depencency started up in the wrong order.

# 2.4.0

## Starting many keys as a map

Now you can pass a map as the key argument to start many keys:

```clojure
(t/deftest lookup-test
  (with-open [root (di/start {:a `a :b `b})]
    (let [{:keys [a b]} root]
      (t/is (= :a a))
      (t/is (= :b b)))))
```

# 2.3.0

## Env parsing

With `di/env-parsing` middleware, you can add env parsers.

```clojure
(defn jetty
  {::di/stop (memfn stop)}
  [{port    :env.long/PORT
    handler `handler
    :or  {port 8080}}]
  (jetty/run-jetty handler {:join? false
                            :port port}))

(di/start `jetty (di/env-parsing {:env.long parse-long}))
```

# 2.2.0

## Multimethods as services

Now you can define a service with a multimethod:

```clojure
(defmulti service
  {::di/deps [::x]}
  (fn [-deps kind] kind))
```

# 2.1.0

## Starting many keys

Now you can pass a vector as the key argument to start many keys:

```clojure
(with-open [root (di/start [`handler `helper])]
  (let [[handler helper] root]
     ...))
```

# 2.0.0

Base version.

# 1.x

Don't use it. I forgot the details.
