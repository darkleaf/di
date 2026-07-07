# Example app

A minimal but realistic web service wired together with DI. It runs a
Jetty server, routes requests via Reitit, queries an H2 database
through HikariCP, and applies Flyway migrations on startup.

The source lives in the repository:

- [example/](https://github.com/darkleaf/di/tree/master/example) — full sources
- [example/dev/user.clj](https://github.com/darkleaf/di/blob/master/example/dev/user.clj) — start here in the REPL
- [example/src/example/system.clj](https://github.com/darkleaf/di/blob/master/example/src/example/system.clj) — registry composition

## What it demonstrates

The example is intentionally small but exercises most of the features
covered in the tutorial:

- **System lifecycle** — `di/start` / `di/stop` from `user.clj`.
- **Components with cleanup** — `{::di/stop ...}` metadata on
  `jetty/server` (`.stop`) and `hikari/datasource` (`close-datasource`).
- **`:component` kind** — explicit
  `{::di/kind :component}` for `reitit/handler` and `flyway/migrate`.
- **Plain services** — `root-handler` is a regular function that takes
  its deps as the first argument.
- **Three key shapes side by side:**
  - symbols (``` `hikari/datasource ```) resolve to vars,
  - keywords (`::handler`, `::options`) represent abstract
    dependencies that the registry binds,
  - strings (`"PORT"`, `"H2_URL"`) are environment variables.
- **`di/env-parsing`** — registers a `:env.long` parser so that
  `port :env.long/PORT` arrives as a number.
- **`di/template`** — embeds `di/ref` calls inside data structures
  (Reitit route data and Hikari options).
- **`di/update-key`** — extends the shared `reitit/route-data` from
  `example.core` so each subsystem can append its own routes without
  the routing component knowing about them.
- **`di/prepend-side-dependency`** — pulls in `flyway/migrate` so
  migrations run before the rest of the system, even though
  nothing references the migrator directly — see
  [Side dependencies](/doc/how_to/side_dependencies_test.md).
- **Registry composition** — `base-registry` returns a vector of maps
  and middlewares. `dev-registry` layers dev-only env values on top.

## Running it

From the `example/` directory:

```
clj -M:dev
user=> (start)
;; open http://localhost:8888
user=> (stop)
```

Redefine `example.core/root-handler` at the REPL and re-evaluate — the
running system picks up the new implementation without a restart, as
described in the [Interactive development](/doc/tutorial/d_interactive_development_test.md) chapter.
