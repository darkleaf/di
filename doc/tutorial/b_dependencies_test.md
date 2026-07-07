# Dependencies

```clojure
(ns darkleaf.di.tutorial.b-dependencies-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.utils :as u]))
```

The previous chapter built a single component on its own. Real
systems are graphs: components depend on other components. This
chapter shows how a component declares its dependencies and how
DI resolves them at start.

## Declaring dependencies

DI uses Clojure's
[associative destructuring](https://clojure.org/guides/destructuring#_associative_destructuring)
to read a component's dependencies. Keys can be symbols,
keywords, or strings — this chapter uses only symbols, which DI
resolves to vars by name.

`root` below depends on `a` and `b`. `b` is optional via `:or`
with `::default` as fallback. The full dependency map is also
available through `:as deps`.

```clojure
(defn root
  {::di/kind :component}
  [{a      `a
    ::syms [b]
    :or    {b ::default}
    :as    deps}]
  [:root a b deps])

(def a ::a)

(t/deftest root-test
  (with-open [root (di/start `root)]
    (t/is (= [:root ::a ::default {`a ::a}] @root))))
```

Two equivalent forms in the destructuring map: ``{a `a}`` binds
the value of key `` `a `` to the local `a`. `::syms [b]` is a
shorthand for several symbols at once. Use whichever reads
better.

## Substituting a dependency

`di/start` accepts a second argument — a map that supplies or
overrides values by key:

```clojure
(t/deftest root-with-extra-deps-test
  (with-open [root (di/start `root {`b ::b})]
    (t/is (= [:root ::a ::b {`a ::a `b ::b}] @root))))
```

This map is called a *registry*. Use it to override what DI
would otherwise resolve from a var — a fake datasource in
tests, a different implementation in dev, and so on.
Registries are covered in detail in
[Registries](/doc/tutorial/e_registries_test.md).

## Required by default

Dependencies are required unless `:or` declares a default. A
missing required dependency makes `di/start` throw. The
exception carries enough information to locate the missing
dependency: the failure `:type` and a `:stack` of keys DI was
resolving — from the missing key (head) up through its parents
to the root.

```clojure
(defn root'
  {::di/kind :component}
  [{a `a'}]
  [::root a])

(t/deftest root'-test
  (let [ex (u/catch-some (di/start `root'))]
    (t/is (= "Missing dependency darkleaf.di.tutorial.b-dependencies-test/a'"
             (ex-message ex)))
    (t/is (= {:type  ::di/missing-dependency
              :stack [`a' `root']}
             (ex-data ex)))))
```

The next chapter covers how a component cleans up when the
system stops.
