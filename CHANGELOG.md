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
