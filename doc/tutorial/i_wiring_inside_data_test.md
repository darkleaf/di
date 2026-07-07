# Wiring inside data

```clojure
(ns darkleaf.di.tutorial.i-wiring-inside-data-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))
```

Many Clojure libraries are configured via data — reitit routes,
scheduler tables, connection-pool maps. DI lets you embed
`(di/ref ...)` directly inside that data. `di/template` walks
the structure on start and replaces each ref with the value it
points at.

## Embedding refs in data

Below, `routes` is reitit-style data with handler references at
each leaf. The handlers are real services with their own
dependencies. At start, DI resolves every `di/ref` — the
handlers in the produced data already have `::datasource`
baked in.

```clojure
(defn list-users [{ds ::datasource} -req]
  (->> ds vals sort vec))

(defn show-user [{ds ::datasource} req]
  (ds (-> req :path-params :id)))

(defn create-user [{ds ::datasource} -req]
  {:status :created, :existing-count (count ds)})

(def routes
  (di/template
   [["/users"     {:get  (di/ref `list-users)
                   :post (di/ref `create-user)}]
    ["/users/:id" {:get  (di/ref `show-user)}]]))

(t/deftest routes-test
  (di/with-open [[routes list-users create-user show-user]
                 (di/start [`routes `list-users `create-user `show-user]
                           {::datasource {"1" "Alice", "2" "Bob"}})]
    (t/is (= [["/users"     {:get  list-users
                             :post create-user}]
              ["/users/:id" {:get  show-user}]]
             routes))))
```

`di/template` walks any nested structure — vectors, maps, sets
— and resolves every `di/ref` it finds. Everything else passes
through unchanged.

## Optional refs

`di/opt-ref` resolves to `nil` when the key has no value.
Useful when a data DSL has slots that may or may not be filled.

```clojure
(def maybe-handler
  (di/template
   {:get (di/opt-ref `nonexistent-handler)}))

(t/deftest opt-ref-test
  (with-open [root (di/start `maybe-handler)]
    (t/is (= {:get nil} @root))))
```

The next chapter covers `di/derive` — transforming a built
value with a function, useful for parsing, normalizing, or
wrapping the result of another dependency.
