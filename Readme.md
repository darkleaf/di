[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.darkleaf/di.svg)](https://clojars.org/org.clojars.darkleaf/di)

# Dependency injection

DI is a dependency injection framework that allows you to define dependencies as cheaply as defining function arguments.

It uses plain clojure functions and [associative destructuring](https://clojure.org/guides/destructuring#_associative_destructuring)
to define a graph of functions and stateful objects.

```clojure
(defn handler [{get-user `get-user} ring-req]
  ...
  (get-user user-id)
  ...)

(defn get-user [{ds `db/datasource} id]
  ...)

(defn jetty [{handler `handler
              port    "PORT"}]
  ...)

(di/start `jetty)
```

## Versions

* See `1.0` branch for previous version
* `master` contains dev version

## Resources

Docs:

* [Step-by-step tutorial](test/darkleaf/di/tutorial)
* [Example app](example/src/example/core.clj)
* [Clj doc](https://cljdoc.org/d/org.clojars.darkleaf/di)
* [Tests](test/darkleaf/di)

## License

Copyright © 2022 Mikhail Kuzmin

Licensed under Eclipse Public License v2.0 (see [LICENSE](LICENSE)).
