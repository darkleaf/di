# Multiple systems

```clojure
(ns darkleaf.di.how-to.multiple-systems-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))
```

Sometimes you need several running systems that share a
subsystem — for example a few independent web apps backed by
the same database pool. Start the shared subsystem first,
deref it for its built value, and pass that value through the
registry of each downstream system. Stop them in reverse
order when you are done.

```clojure
(defn shared
  {::di/kind :component}
  []
  (Object.))

(defn server
  {::di/kind :component}
  [{name   ::name
    shared `shared}]
  [name shared])

(t/deftest multi-system-test
  (with-open [shared (di/start `shared)
              a      (di/start `server {`shared @shared
                                        ::name  :a})
              b      (di/start `server {`shared @shared
                                        ::name  :b})]
    (t/is (= :a (first @a)))
    (t/is (= :b (first @b)))
    ;; both servers see the same shared instance
    (t/is (identical? (second @a) (second @b)))))
```
