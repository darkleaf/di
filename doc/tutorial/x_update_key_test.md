# Update key

```clojure
(ns darkleaf.di.tutorial.x-update-key-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))
```

In most cases you just want to instrument or update one dependency.

```clojure
(def route-data [])

(defn subsystem-a-route-data
  {::di/kind :component}
  [-deps]
  ["/a"])

(t/deftest update-key-test
  (with-open [root (di/start `route-data
                             (di/update-key `route-data conj
                                            (di/ref `subsystem-a-route-data)
                                            ["/b"]
                                            nil)
                             {})]
    (t/is (= [["/a"] ["/b"] nil] @root))))
```
