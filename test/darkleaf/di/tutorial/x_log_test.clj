;; # Log

(ns darkleaf.di.tutorial.x-log-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; `di/log` is a middleware that fires a callback every time a
;; factory is built and every time it is demolished. Each callback
;; receives `{:keys [key object]}` — the key and the built value (or
;; the value about to be demolished).

;; Reach for it to instrument the system at runtime — time each
;; build or demolish step, or stream lifecycle events into your
;; logging system. To analyze build and teardown order without
;; actually running the system, use
;; [`di/inspect`](/doc/tutorial/x_inspect_test.md) instead.

;; `di/log` must be the last middleware in the chain — it wraps every
;; factory the registry exposes, and anything appended after it ends
;; up between `log` and the original factory.

;; The components below form a chain `c → b → a`. Builds run in
;; dependency order; demolitions run in reverse — last built, first
;; demolished. Note also how the printed forms differ: a component
;; shows its built value, a service shows the var it points to.

(defn a
  {::di/kind :component}
  []
  :a)

(defn b [{a `a}]
  :b)

(defn c
  {::di/kind :component}
  [{b `b}]
  :c)

(t/deftest log-test
  (let [logs            (atom [])
        after-build!    (fn [{:keys [key object]}]
                          (swap! logs conj [:built      key (pr-str object)]))
        after-demolish! (fn [{:keys [key object]}]
                          (swap! logs conj [:demolished key (pr-str object)]))
        root            (di/start `c
                                  (di/log :after-build!    after-build!
                                          :after-demolish! after-demolish!))]
    (di/stop root)
    (t/is (= [[:built `a ":a"]
              [:built `b
               "#darkleaf.di.core/service #'darkleaf.di.tutorial.x-log-test/b"]
              [:built `c ":c"]
              [:demolished `c ":c"]
              [:demolished `b
               "#darkleaf.di.core/service #'darkleaf.di.tutorial.x-log-test/b"]
              [:demolished `a ":a"]]
             @logs))))
