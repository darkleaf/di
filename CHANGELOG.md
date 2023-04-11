# 2.2.0

## Multimethods as services

Now you can define a sevice with a multimethod:

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
