;; # Startup checks

(ns darkleaf.di.how-to.startup-checks-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.utils :refer [catch-some]]))

;; A deployment can be broken in a way that no component notices.
;; A new version of the application stopped using Redis, but the
;; deployment still runs the server and sets its connection
;; variable: nothing fails, and the mistake silently persists.
;; Or the database server is too old for the code: the system
;; starts fine, and the failure waits for the first query that
;; needs a missing feature — at run time, far from its cause.

;; A startup check turns such a mistake into an error at system
;; build time: `di/start` throws, the deploy fails, and the log
;; names the fix.
;; DI already fails this way on what it can see — a missing
;; dependency stops the start. A check extends that to what only
;; your code can see.

;; There are two techniques. A freestanding check — nothing in the
;; system references it — becomes a component of its own, pulled
;; in with `di/add-side-dependency`. A check on an existing
;; component attaches to it with `di/update-key`.

;; ## A freestanding check

;; In version 1 the application cached in Redis. In version 2 the
;; cache moved in-process, and the code no longer connects to
;; Redis. But a deployment is more than the application. The
;; operator still runs a Redis server and still sets `REDIS_URL`.
;; An unused variable is not an error, so the upgrade succeeds.
;; `app` stands in for the whole application — in a real project
;; a tree of many components stands behind this stub:

(defn app
  {::di/kind :component}
  []
  :app)

;; A string key is an environment variable
;; ([Environment variables](/doc/tutorial/g_environment_variables_test.md));
;; in tests a map registry stands in for the real environment.

;; The check is a component whose whole job is to throw when the
;; mistake is present. It declares the forbidden variable as an
;; optional dependency. The `:or` matters — a check must not
;; require what it forbids. The message is written for the
;; operator reading the log of a failed deploy: it says what to
;; shut down and what to remove. The return value never matters:

(defn redis-removed
  {::di/kind :component}
  [{url "REDIS_URL"
    :or   {url nil}}]
  (when (some? url)
    (throw (ex-info "Redis is no longer used. Stop the server and remove REDIS_URL"
                    {:REDIS_URL url}))))

;; Nothing references the check, so `di/add-side-dependency` pulls
;; it into the system
;; ([Side dependencies](/doc/how_to/side_dependencies_test.md)).
;; When the check fires, the components built before it are
;; stopped before the error propagates
;; ([Handling start failures](/doc/tutorial/l_handling_start_failures_test.md)):

(t/deftest removed-redis-test
  (let [ex (catch-some (di/start `app
                                 (di/add-side-dependency `redis-removed)
                                 {"REDIS_URL" "redis://cache.internal"}))]
    ;; `di/start` wraps the failure, the check's exception is the cause
    (t/is (= "Redis is no longer used. Stop the server and remove REDIS_URL"
             (-> ex ex-cause ex-message)))))

;; A clean deployment — no `REDIS_URL` — passes the check and
;; never notices it.

;; The same shape guards other mistakes. A renamed variable that
;; deployments still set under the old name. A known misspelling
;; of a flag variable that would silently leave the feature off.

;; ## A check on a built component

;; The second technique inspects a component that already
;; exists. The classic case is a version requirement: the code
;; relies on features of the database server, so a server that is
;; too old must fail the start, not the first query that hits the
;; missing feature.

;; `db` stands in for a connection component. A real one opens a
;; connection. The stub carries only what the check reads:

(defn db
  {::di/kind :component}
  []
  {:version 14})

;; The check is a plain function. It receives the built object,
;; throws or does not, and returns the object untouched:

(defn check-db-version [db]
  ;; a real check queries the server
  (let [version (:version db)]
    (when (< version 15)
      (throw (ex-info "Update the database server to version 15 or higher"
                      {:version version}))))
  db)

;; `di/update-key` attaches the check to the component
;; ([Composition with update-key](/doc/tutorial/k_composition_with_update_key_test.md)).
;; The stub is too old, so the start fails:

(t/deftest version-too-old-test
  (let [ex (catch-some (di/start `db (di/update-key `db check-db-version)))]
    (t/is (= "Update the database server to version 15 or higher"
             (-> ex ex-cause ex-message)))))

;; ## Why not a component that depends on `db`?

;; The version check could be written like the first one — a
;; micro-component that depends on `db`, added as a side
;; dependency. The difference is what it forces to exist. A side
;; dependency is always built, and it demands its dependencies, so
;; that check would force `db` into every system — including one
;; where a feature flag leaves the database out
;; ([Feature flags](/doc/how_to/feature_flags_test.md)).
;; `di/update-key` wraps the component's own factory. The check
;; runs exactly when `db` is built and never forces it to be.

;; ## The checks registry

;; Checks accumulate over the life of a project: a removed
;; dependency in this release, a version requirement in the next.
;; Collect them in one namespace with a registry function, and add
;; it to the chain like any other subsystem:

;; ```clojure
;; (ns app.checks ...)
;;
;; (defn registry []
;;   [(di/add-side-dependency `redis-removed)
;;    (di/update-key `app.db/db check-db-version)])
;;
;; ;; main system
;; (di/start `app
;;           (web/registry)
;;           (checks/registry))
;; ```
