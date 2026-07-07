[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.darkleaf/di.svg)](https://clojars.org/org.clojars.darkleaf/di)
[![cljdoc badge](https://cljdoc.org/badge/org.clojars.darkleaf/di)](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT)

# Dependency injection

[DI](https://github.com/darkleaf/di) is a dependency injection framework
that allows you to define dependencies as easily as you define function arguments.

It uses plain clojure functions and [associative destructuring](https://clojure.org/guides/destructuring#_associative_destructuring)
to define a graph of functions and stateful objects.

```clojure
(ns app.core
  (:require
   [darkleaf.di.core :as di]
   [ring.adapter.jetty :as jetty]
   [app.adapters.reitit :as-alias reitit]
   [app.adapters.hikari :as-alias hikari]
   [app.adapters.db     :as-alias db]))

(defn show-user [{ds ::db/datasource} req]
  ...)

(def route-data
  (di/template
    [["/users/:id" {:get {:handler (di/ref `show-user)}}]]))

(defn jetty
  {::di/stop (memfn stop)}
  [{handler ::handler
    port    :env.long/PORT
    :or     {port 8080}}]
  (jetty/run-jetty handler {:join? false, :port port}))

(di/start `jetty
          (di/env-parsing :env.long parse-long)
          {::handler           (di/ref `reitit/handler)
           ::reitit/route-data (di/ref `reitit/data)
           ::db/datasource     (di/ref `hikari/datasource)
           "PORT"              "9090"})
```

For a complete application, see the [example app](https://github.com/darkleaf/di/tree/master/example).

## Install

```edn
{:deps {org.clojars.darkleaf/di {:mvn/version "3.7.0"}}}
;; or
{:deps {org.clojars.darkleaf/di {:git/url "https://github.com/darkleaf/di.git"
                                 :sha     "a78e011f48030d22e20d338540a8ee6dd9250147"}}}
```

## Documentation

Full documentation, tutorials, and API reference are available on
[cljdoc](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT).

See also the [example app](https://github.com/darkleaf/di/tree/master/example),
starting with [user.clj](https://github.com/darkleaf/di/blob/master/example/dev/user.clj).

## Versions

* See `1.0` branch for previous version
* See `master` branch for current version

## License

Copyright © 2022 Mikhail Kuzmin

Licensed under Eclipse Public License v2.0 (see [LICENSE](https://github.com/darkleaf/di/blob/master/LICENSE)).
