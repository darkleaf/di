# Feature flags

```clojure
(ns darkleaf.di.how-to.feature-flags-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))
```

Parts of an application can be optional: a feature sold in a
paid plan, an integration that only some deployments configure.
A feature flag decides whether such a part runs.

A flag is an `if`. The usual place for that `if` is the running
code: read the flag, branch. This recipe moves it to build time.
A registry function reads the flag once, while the system is
assembled, and decides what to build. The components it wires
carry no flag check. A disabled feature is not built at all.

Two techniques do this. A feature others depend on is
substituted with a null object. A feature that only adds
components is dropped by a conditional registry. The last
section shows where the flags themselves come from — a
bootstrap start.

## The example system

The example is a small web application with two flagged
features: a shop and a geoip lookup. The shop contributes a
route and a handler. The handler shows the shipping city, so it
calls the geoip lookup. The lookup resolves an IP address to a
city. The real implementation opens a database — a file the
operator downloads and points the application at. When geoip is
off, the application must not demand any of that. The full plan
enables both features. The lite plan runs without them.

Everything sits in this one test namespace to stay runnable. In
a real project each part is its own namespace: the web adapter
owns the route table, each subsystem owns its components and
its registry function, and the system namespace assembles them.
The sections below follow that split.

The route table starts empty. Subsystems extend it:

```clojure
(def routes [])
```

Flags are a plain map: namespaced keywords, boolean values.
Nothing di-specific yet.

```clojure
(def full-flags
  #:app.features{:shop-enabled  true
                 :geoip-enabled true})

(def lite-flags
  #:app.features{:shop-enabled  false
                 :geoip-enabled false})
```

## The geoip subsystem

Each subsystem defines a `registry` function. It takes the
flags and decides what to contribute.

`geoip` has consumers — the shop is one. Dropping the component
would break them, so the registry substitutes it instead. The
lookup is a protocol. The real implementation and a null object
both honor it:

```clojure
(defprotocol GeoIP
  (lookup-city [this ip]))

(defn geoip-impl
  {::di/kind :component}
  [{path "GEOIP_DB_PATH"}]
  ;; the real implementation opens the database at `path`
  (reify GeoIP
    (lookup-city [_ ip]
      "Berlin")))

(defn null-geoip
  {::di/kind :component}
  []
  (reify GeoIP
    (lookup-city [_ _ip]
      "unknown")))
```

The registry binds the `geoip` key explicitly in both states.
Consumers depend on `geoip` and never check the flag. The `if`
fails safe: if the flag is renamed or misspelled, reading it
yields `nil`, and the key goes to the null object. A broken
flag turns the feature off, not on:

```clojure
(defn geoip-registry [{:app.features/keys [geoip-enabled]}]
  {`geoip (di/ref (if geoip-enabled
                    `geoip-impl
                    `null-geoip))})
```

## The shop subsystem

The shop is the other case: nothing depends on it. A disabled
shop can simply be absent.

The handler needs the city, so it depends on `geoip`. It
carries no flag check:

```clojure
(defn shop-handler [{geoip `geoip} request]
  {:status 200
   :body   (str "shipping to " (lookup-city geoip (:remote-addr request)))})
```

The route pairs a path with the handler, so the contribution is
a template
([Wiring inside data](/doc/tutorial/i_wiring_inside_data_test.md)) —
`di/update-key` accepts factories as arguments. The registry
wraps the entry in `when`. A disabled entry is `nil`, and `nil`
is a valid middleware — a no-op
([The middleware argument](/doc/reference/middleware_argument.md)).
With this shape one vector holds any number of conditional
entries ([Tips](/doc/how_to/tips_test.md)):

```clojure
(defn shop-registry [{:app.features/keys [shop-enabled]}]
  [(when shop-enabled
     (di/update-key `routes conj
                    (di/template ["/shop" (di/ref `shop-handler)])))])
```

## The application registry

The top-level registry function passes the flags to every
subsystem:

```clojure
(defn app-registry [flags]
  [#_flags
   (geoip-registry flags)
   (shop-registry flags)])
```

The commented `flags` line is an option. A map is a registry of
constants, so adding the map itself to the chain turns every
flag into a component. Do that when the running system needs
the values: to introspect them, or to report enabled features
to a client. But do not branch on a flag at run time — that
puts the `if` back into the running code.

## Testing a flagged system

Run the test suite against the full configuration — every
feature on — and share that system across the tests with
`di/->memoize`
([Reusing components between tests](/doc/how_to/reusing_components_between_tests_test.md)).

The memoized registry is created from one flag configuration and
serves only that shape of the system. Tests for the other shape
do not need a second cache, because they do not need a whole
system. Any component can be a root. The null object starts
alone:

```clojure
(t/deftest null-geoip-test
  (with-open [geoip (di/start `null-geoip)]
    (t/is (= "unknown" (lookup-city @geoip "1.2.3.4")))))
```

The lite plan needs a different kind of test. A license is a
promise about composition: the deployment must not open features
the customer did not buy. To be sure of that, there is no need
to run the application. It is enough to look at what the system
is made of and compare it with a whitelist. The full plan has
no whitelist to violate — it enables everything — and the suite
already runs against it.

`di/inspect`
([Inspect](/doc/reference/inspect_test.md)) takes the same
arguments as `di/start` and reports the graph the runtime would
build, without building anything. The example system is a
handful of keys, so its whitelist is the literal key set:

```clojure
(defn- plan-keys [flags]
  (into #{}
        (map :key)
        (di/inspect `routes (app-registry flags))))

(t/deftest lite-plan-test
  ;; no shop, no geoip, no GEOIP_DB_PATH — a lite deployment does
  ;; not have to provide the database or the variable at all
  (t/is (= #{`routes}
           (plan-keys lite-flags))))
```

A real system has too many keys to list, and the list would
break on every new component. Aggregate the report instead.
Constants supplied by the registries report themselves as
`:trivial` — drop them. Map every qualified key — symbol or
keyword, `qualified-ident?` covers both — to its namespace, and
keep string keys as they are. The result is one set of
namespaces and environment variables. A new component in an
existing subsystem does not change it. It changes when the
plan's composition changes — a new subsystem, a new variable —
and that is what the whitelist must catch:

```clojure
(into #{}
      (comp
       (remove #(= :trivial (-> % :description ::di/kind)))
       (map (fn [{:keys [key]}]
              (if (qualified-ident? key)
                (namespace key)
                key))))
      (di/inspect `routes (app-registry flags)))
```

The variables in that set are the configuration contract of the
deployment: a deployment that misses one fails at `di/start`,
before any traffic. So the test also guards backward
compatibility — a new required variable is a breaking change
for every existing deployment, and it must be a deliberate
decision, not a side effect of wiring. List the test file in
CODEOWNERS: adding a variable then requires an explicit review.

## Flag sources

In the tests above the flags were literals. In production they
are computed: from environment variables, from a license key
that encodes the customer's plan. The natural first attempt is
to make `flags` a component of the system. It does not work.
The registries need the flags before `di/start` — the flags
decide what the registries are.

The way out is a bootstrap start. `flags` is a component — of a
separate, tiny system that starts first. Its built value is a
plain map: pass it to the registry functions of the real
system.

The flag variables go through `di/env-parsing`
([Environment variables](/doc/tutorial/g_environment_variables_test.md)).
The bootstrap is a separate system, but it can reuse some
middlewares of the real one — here the configured
`env-parsing` — so variables follow one parsing convention
everywhere. A missing
variable defaults to off — the same fail-safe rule as the geoip
binding:

```clojure
(def env-parsing-registry
  (di/env-parsing :env.bool parse-boolean))

(defn flags
  {::di/kind :component}
  [{shop-enabled  :env.bool/SHOP_ENABLED
    geoip-enabled :env.bool/GEOIP_ENABLED
    :or           {shop-enabled  false
                   geoip-enabled false}}]
  #:app.features{:shop-enabled  shop-enabled
                 :geoip-enabled geoip-enabled})

(t/deftest bootstrap-test
  (with-open [flags   (di/start `flags
                                env-parsing-registry
                                {"SHOP_ENABLED" "true"})
              routes (di/start `routes (app-registry @flags))]
    ;; GEOIP_ENABLED is not set, so the flag defaulted to off
    (t/is (= false (:app.features/geoip-enabled @flags)))
    (t/is (= ["/shop"] (mapv first @routes)))))
```
