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

## Resources

Docs:

* [Step-by-step tutorial](test/darkleaf/di/tutorial)
* [Example app](example/src/example/core.clj)
* Api docs
* [Tests](test/darkleaf/di)

## License

Copyright © 2022 Mikhail Kuzmin

Licensed under Eclipse Public License v2.0 (see [LICENSE](LICENSE)).
