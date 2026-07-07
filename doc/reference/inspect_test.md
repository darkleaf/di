# Inspect

```clojure
(ns darkleaf.di.reference.inspect-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]
   [darkleaf.di.how-to.ns-publics-test :as x-ns-publics-test]))
```

`di/inspect` takes the same arguments as `di/start` but builds
nothing. It walks the registry and returns a vector describing
every factory the runtime would visit — keys, their declared
dependencies, and the `description` map each `Factory` exposes.

Reach for it when you want to verify how middlewares reshape the
system, debug a wiring mismatch, or feed a dependency-graph
visualizer.

## Environment variables

A string key resolves to an environment variable. The root of the
inspected graph is always marked with `::di/root true`.

```clojure
(t/deftest env-test
  (t/is (= [{:key         "FOO"
             :description {::di/kind :env
                           ::di/root true}}]
           (di/inspect "FOO"))))
```

A map middleware overrides the lookup — the description shifts from
`:env` to `:trivial` because the value now comes from the literal map.

```clojure
(t/deftest fixed-env-test
  (t/is (= [{:key         "FOO"
             :description {::di/kind :trivial
                           :object   "value"
                           ::di/root true}}]
           (di/inspect "FOO" {"FOO" "value"}))))
```

## Vars

A symbol key resolves to a Clojure var. A plain `def` is reported
as a trivial factory whose `:object` is the var's value. The var
itself appears under `::di/variable`.

```clojure
(def variable :obj)

(t/deftest variable-test
  (t/is (= [{:key         `variable
             :description {::di/kind     :trivial
                           :object       :obj
                           ::di/root     true
                           ::di/variable #'variable}}]
           (di/inspect `variable))))
```

When a var holds a `Factory` instance, `inspect` calls that
factory's own `description` method. An empty description still
leaves `::di/variable` in place.

```clojure
(def variable+factory
  (reify p/Factory
    (dependencies [_])
    (build [_ _ _] :ok)
    (description [_] {})))

(t/deftest variable+factory-test
  (t/is (= [{:key         `variable+factory
             :description {::di/root     true
                           ::di/variable #'variable+factory}}]
           (di/inspect `variable+factory))))
```

A custom description is passed through unchanged.

```clojure
(def variable+description
  (reify p/Factory
    (dependencies [_])
    (build [_ _ _] :ok)
    (description [_]
      {::di/kind ::variable+description})))

(t/deftest variable+description-test
  (t/is (= [{:key         `variable+description
             :description {::di/kind     ::variable+description
                           ::di/root     true
                           ::di/variable #'variable+description}}]
           (di/inspect `variable+description))))
```

The same applies to a `di/template` stored in a var.

```clojure
(def variable+template
  (di/template [42]))

(t/deftest variable+template-test
  (t/is (= [{:key         `variable+template
             :description {::di/kind     :template
                           :template     [42]
                           ::di/root     true
                           ::di/variable #'variable+template}}]
           (di/inspect `variable+template))))
```

## Components and services

A `defn` becomes a `:component` when it carries `{::di/kind
:component}` metadata and a `:service` otherwise. Arity doesn't
affect what `inspect` reports.

```clojure
(defn component-0-arity
  {::di/kind :component}
  []
  :ok)

(t/deftest component-0-arity-test
  (t/is (= [{:key         `component-0-arity
             :description {::di/kind     :component
                           ::di/root     true
                           ::di/variable #'component-0-arity}}]
           (di/inspect `component-0-arity))))


(defn component-1-arity
  {::di/kind :component}
  [-deps]
  :ok)

(t/deftest component-1-arity-test
  (t/is (= [{:key         `component-1-arity
             :description {::di/kind     :component
                           ::di/root     true
                           ::di/variable #'component-1-arity}}]
           (di/inspect `component-1-arity))))


(defn service-0-arity
  {::di/kind :service}
  []
  :ok)

(t/deftest service-0-arity-test
  (t/is (= [{:key         `service-0-arity
             :description {::di/kind     :service
                           ::di/root     true
                           ::di/variable #'service-0-arity}}]
           (di/inspect `service-0-arity))))


(defn service-n-arity
  {::di/kind :service}
  [-deps]
  :ok)

(t/deftest service-n-arity-test
  (t/is (= [{:key         `service-n-arity
             :description {::di/kind     :service
                           ::di/root     true
                           ::di/variable #'service-n-arity}}]
           (di/inspect `service-n-arity))))
```

Multimethods follow the same rules — `::di/deps` declares their
dependencies.

```clojure
(defmulti multimethod-service
  {::di/deps []}
  (fn [-deps kind] kind))

(t/deftest multimethod-service-test
  (t/is (= [{:key         `multimethod-service
             :description {::di/kind     :service
                           ::di/root     true
                           ::di/variable #'multimethod-service}}]
           (di/inspect `multimethod-service))))
```

## Refs, templates, and derives

The composite factories built by `di/ref`, `di/template`, and
`di/derive` expose their inner shape under `:description`, alongside
the keys they pull in under `:dependencies`. Unresolved keys show
up as `:undefined`.

```clojure
(t/deftest ref-test
  (t/is (= [{:key          `foo
             :dependencies {`bar :required}
             :description  {::di/kind :ref
                            :key      `bar
                            :type     :required
                            ::di/root true}}
            {:key         `bar
             :description {::di/kind :undefined}}]
           (di/inspect `foo {`foo (di/ref `bar)}))))


(t/deftest template-test
  (t/is (= [{:key          `foo
             :dependencies {`bar :required}
             :description  {::di/kind :template
                            :template [42 (di/ref `bar)]
                            ::di/root true}}
            {:key         `bar
             :description {::di/kind :undefined}}]
           (di/inspect `foo {`foo (di/template [42 (di/ref `bar)])}))))


(t/deftest derive-test
  (t/is (= [{:key          `foo
             :dependencies {`bar :optional}
             :description  {::di/kind :derive
                            :key      `bar
                            :f        str
                            :args     ["arg"]
                            ::di/root true}}
            {:key         `bar
             :description {::di/kind :undefined}}]
           (di/inspect `foo {`foo (di/derive `bar str "arg")}))))
```

## Trivial values

Anything that isn't a `Factory` becomes a trivial wrapper. The
original value lives under `:object`.

```clojure
(t/deftest trivial-nil-test
  (t/is (= [{:key         `foo
             :description {::di/kind :trivial
                           :object   nil
                           ::di/root true}}]
           (di/inspect `foo {`foo nil}))))


(t/deftest trivial-obj-test
  (t/is (= [{:key         `foo
             :description {::di/kind :trivial
                           :object   str
                           ::di/root true}}]
           (di/inspect `foo {`foo str}))))
```

## Middlewares modify descriptions

Many middlewares attach extra fields to the factories they wrap.
`inspect` surfaces all of them, which is the easiest way to confirm
that the wiring matches your intent.

`di/update-key` records the chain of modifications under
`::di/update-key`.

```clojure
(t/deftest update-key-test
  (t/is (= [{:key          `a
             :dependencies {`b :required}
             :description  {::di/kind :trivial
                            :object   :obj
                            ::di/root true
                            ::di/update-key
                            [[{::di/kind :trivial
                               :object   str}
                              {::di/kind :ref
                               :key      `b
                               :type     :required}]
                             [{::di/kind :trivial
                               :object   identity}]]}}
            {:key         `b
             :description {::di/kind :trivial
                           :object   "b"}}]
           (di/inspect `a
                       {`a :obj
                        `b "b"}
                       (di/update-key `a str (di/ref `b))
                       (di/update-key `a identity)))))
```

`di/add-side-dependency` and `di/prepend-side-dependency` mark
the pulled-in keys with `::di/side-dependency true`. The
entries follow the build order: prepended keys come before the
root, added keys after it.

```clojure
(t/deftest side-dependency-test
  (t/is (= [{:key         `prepended-dep
             :description {::di/kind            :trivial
                           :object              :side-dep
                           ::di/side-dependency true}}
            {:key         `a
             :description {::di/kind :trivial
                           :object   :obj
                           ::di/root true}}
            {:key         `added-dep-1
             :description {::di/kind            :trivial
                           :object              :side-dep
                           ::di/side-dependency true}}
            {:key         `added-dep-2
             :description {::di/kind            :trivial
                           :object              :side-dep
                           ::di/side-dependency true}}]
           (di/inspect `a
                       {`a             :obj
                        `prepended-dep :side-dep
                        `added-dep-1   :side-dep
                        `added-dep-2   :side-dep}
                       (di/prepend-side-dependency `prepended-dep)
                       (di/add-side-dependency `added-dep-1)
                       (di/add-side-dependency `added-dep-2)))))
```

`di/ns-publics` and `di/env-parsing` show up as standalone
`:middleware` factories standing in front of the keys they expose.

```clojure
(t/deftest ns-publics-test
  (t/is (= [{:key          :ns-publics/darkleaf.di.how-to.ns-publics-test
             :dependencies {`x-ns-publics-test/service   :required
                            `x-ns-publics-test/component :required
                            `x-ns-publics-test/ok-test   :required}
             :description  {::di/kind   :middleware
                            :middleware ::di/ns-publics
                            :ns         'darkleaf.di.how-to.ns-publics-test
                            ::di/root   true}}
            {:key          `x-ns-publics-test/service
             :dependencies {`x-ns-publics-test/component :required}
             :description  {::di/kind     :service
                            ::di/variable #'x-ns-publics-test/service}}
            {:key         `x-ns-publics-test/component
             :description {::di/kind     :component
                           ::di/variable #'x-ns-publics-test/component}}
            {:key         `x-ns-publics-test/ok-test
             :description {::di/kind     :trivial
                           :object       x-ns-publics-test/ok-test
                           ::di/variable #'x-ns-publics-test/ok-test}}]
           (di/inspect :ns-publics/darkleaf.di.how-to.ns-publics-test
                       (di/ns-publics)))))


(t/deftest env-parsing-test
  (t/is (= [{:key          :env.long/PORT
             :dependencies {"PORT" :optional}
             :description  {::di/kind   :middleware
                            :middleware ::di/env-parsing
                            :cmap       {:env.long parse-long}
                            ::di/root   true}}
            {:key         "PORT"
             :description {::di/kind :trivial
                           :object   "8080"}}]
           (di/inspect :env.long/PORT
                       (di/env-parsing :env.long parse-long)
                       {"PORT" "8080"}))))
```

`di/log` adds `::di/log` to every factory it wraps.
The `#_#_:opts nil` form below is commented-out code: `:opts`
is not part of the description today, but a future version may
report the logger options under that key.

```clojure
(t/deftest log-test
  (t/is (= [{:key         `foo
             :description {::di/kind :trivial
                           :object   :obj
                           ::di/log  {:will-be-logged true
                                      #_#_:opts       nil}
                           ::di/root true}}]
           (di/inspect `foo
                       {`foo :obj}
                       (di/log)))))
```

## Multiple roots

Pass a vector or a map as the first argument to inspect several
roots at once.

```clojure
(t/deftest vector-test
  (t/is (= [{:key "A"
             :description {::di/kind :env
                           ::di/root true}}
            {:key "B"
             :description {::di/kind :env
                           ::di/root true}}]
           (di/inspect ["A" "B"]))))


(t/deftest map-test
  (t/is (= [{:key         "A"
             :description {::di/kind :env
                           ::di/root true}}
            {:key         "B"
             :description {::di/kind :env
                           ::di/root true}}]
           (di/inspect {:a "A" :b "B"}))))
```
