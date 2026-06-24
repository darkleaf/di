;; # Logging system lifecycle

(ns darkleaf.di.how-to.log-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; `di/log` fires a callback every time a factory is built and
;; every time it is demolished. Each callback receives
;; `{:keys [key object]}` — the key and the built value (or the
;; value about to be demolished).

;; Reach for it to instrument the system at runtime — time each
;; build or demolish step, or stream lifecycle events into your
;; logging system. To analyze build and teardown order without
;; actually running the system, use
;; [`di/inspect`](/doc/tutorial/x_inspect_test.md) instead.

;; Put `di/log` last when you call `di/start`. `log` reports
;; every factory before it in the argument list. Anything after
;; `log` is not reported.

;; The components below form a chain `c → b → a`. Builds run in
;; dependency order. Demolitions run in reverse — last built,
;; first demolished. The printed forms also differ: a component
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

;; The callbacks log via `pr-str` because service `b` builds to
;; a partial fn — not `=`-comparable to a literal, but it has a
;; custom print method that yields a stable string.

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
               "#darkleaf.di.core/service #'darkleaf.di.how-to.log-test/b"]
              [:built `c ":c"]
              [:demolished `c ":c"]
              [:demolished `b
               "#darkleaf.di.core/service #'darkleaf.di.how-to.log-test/b"]
              [:demolished `a ":a"]]
             @logs))))
