;; # Environment variables

(ns darkleaf.di.tutorial.g-environment-variables-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Symbols resolve to vars; keywords to abstractions in the
;; registry. The third kind of key — a string — resolves to an
;; environment variable. This is how you plug a system into the
;; environment it runs in: container env, .env file, shell, CI
;; secrets.

;; ## String keys → env vars

;; Declare a string in the destructuring map. DI looks the value
;; up from `System/getenv`.

(defn root
  {::di/kind :component}
  [{path "PATH"}]
  [:root path])

(def PATH (System/getenv "PATH"))

(t/deftest root-test
  (with-open [root (di/start `root)]
    (t/is (= [:root PATH] @root))))

;; The value is always a string — the way the OS sees it. To get
;; numbers, booleans, JSON, edn, parse them on the way in.

;; ## Typed values via `di/env-parsing`

;; Pass `(di/env-parsing ...)` to `di/start` and you can depend
;; on a qualified keyword like `:env.long/PORT`. DI looks up the
;; env var by name (`PORT`) and passes its string through the
;; parser registered for the keyword namespace (`:env.long` →
;; `parse-long`).

(defn jetty
  {::di/kind :component}
  [{port :env.long/PORT
    :or  {port 8080}}]
  [:jetty port])

(t/deftest jetty-test
  ;; PORT is not set — :or default kicks in
  (with-open [jetty (di/start `jetty
                              (di/env-parsing {:env.long parse-long}))]
    (t/is (= [:jetty 8080] @jetty)))
  ;; PORT supplied as a string in a registry — parsed to a number
  (with-open [jetty (di/start `jetty
                              (di/env-parsing :env.long parse-long)
                              {"PORT" "8081"})]
    (t/is (= [:jetty 8081] @jetty))))

;; Both the kwargs form (`:env.long parse-long`) and the map form
;; (`{:env.long parse-long}`) work. Register as many keyword
;; namespaces as you need: `:env.long`, `:env.bool`, `:env.json`,
;; anything.

;; (`di/env-parsing` is one of several registry shapes — see
;; [Middleware types](/doc/reference/middleware_types_test.md)
;; for the wider picture.)

;; ## Required vs optional

;; An env-typed dependency without `:or` is required. If the
;; underlying env var is missing, `di/start` throws.

(defn required-env
  {::di/kind :component}
  [{enabled :env.bool/ENABLED}]
  [:enabled enabled])

(defn optional-env
  {::di/kind :component}
  [{enabled :env.bool/ENABLED
    :or     {enabled true}}]
  [:enabled enabled])

(t/deftest env-test
  ;; required, ENABLED is not set → fails at start
  (t/is (thrown? clojure.lang.ExceptionInfo
                 (di/start `required-env
                           (di/env-parsing {:env.bool #(= "true" %)}))))

  ;; required, ENABLED supplied via registry
  (with-open [sys (di/start `required-env
                            (di/env-parsing {:env.bool #(= "true" %)})
                            {"ENABLED" "false"})]
    (t/is (= [:enabled false] @sys)))

  ;; optional, ENABLED not set → :or default
  (with-open [sys (di/start `optional-env
                            (di/env-parsing {:env.bool #(= "true" %)}))]
    (t/is (= [:enabled true] @sys)))

  ;; optional, ENABLED supplied → overrides the default
  (with-open [sys (di/start `optional-env
                            (di/env-parsing {:env.bool #(= "true" %)})
                            {"ENABLED" "false"})]
    (t/is (= [:enabled false] @sys))))

;; The next chapter shows how to wire components into plain data
;; structures — reitit routes, scheduler tables — using
;; `di/template` and `di/ref`.
