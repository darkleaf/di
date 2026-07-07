;; # Reusing components between tests

(ns darkleaf.di.how-to.reusing-components-between-tests-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.utils :refer [catch-some]]))

;; An integration test starts a system, exercises it, and stops it.
;; Each `di/start` builds every component the test needs. When some
;; of them are expensive — a database connection, an HTTP server, a
;; parser with a heavy dictionary — every test pays that cost again.

;; [`di/->memoize`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#->memoize)
;; returns a stateful middleware
;; ([The middleware argument](/doc/reference/middleware_argument.md))
;; that caches built components across `di/start` calls. Each test
;; starts only the keys it needs. The first test builds the
;; expensive components, the rest take them from the cache.

;; This recipe models a small web stack. The handler depends on a
;; database connection and a parser. The `Object.` instances stand
;; in for real connection state — the tests compare them by
;; identity to tell a cached component from a rebuilt one.

(defn db
  {::di/kind :component}
  [{url "DB_URL"}]
  {:url url, :conn (Object.)})

(defn parser
  {::di/kind :component}
  []
  (Object.))

(defn handler
  {::di/kind :component}
  [{db `db, parser `parser}]
  {:db db, :parser parser})

;; Two tests start the handler against the same memoized registry.
;; Here they are folded into one `deftest` so the recipe stays
;; self-contained.

(t/deftest reuse-test
  (with-open [mem    (di/->memoize {"DB_URL" "jdbc:test"})
              first  (di/start `handler mem)
              second (di/start `handler mem)]
    ;; the second start took every component from the cache
    (t/is (identical? @first @second))))

;; ## Per-test overrides

;; The memoized registry must be the first middleware in
;; `di/start`. Per-test overrides go after it:

(t/deftest mem-goes-first-test
  (with-open [mem (di/->memoize)]
    (let [ex (catch-some (di/start `handler {"DB_URL" "jdbc:test"} mem))]
      (t/is (= ::di/wrong-memoized-registry-position
               (-> ex ex-data :type))))))

;; An override rebuilds only the components that depend on it. The
;; rest still comes from the cache. And overrides are compared by
;; value: pass an equal override again and you get the cached
;; components back.

(t/deftest override-test
  (with-open [mem    (di/->memoize {"DB_URL" "jdbc:test"})
              first  (di/start `handler mem)
              second (di/start `handler mem {"DB_URL" "jdbc:other"})
              third  (di/start `handler mem {"DB_URL" "jdbc:other"})]
    ;; the new url produced a new db...
    (t/is (not (identical? (:db @first) (:db @second))))
    ;; ...while the parser does not depend on it and was reused
    (t/is (identical? (:parser @first) (:parser @second)))
    ;; an equal override hits the cache
    (t/is (identical? @second @third))))

;; ## Who stops what

;; The memoized registry owns everything it builds. Stopping a
;; test's system does not stop cached components. They live until
;; you stop the memoized registry itself with `di/stop`.

;; To observe the lifecycle, this test folds `di/log`
;; ([Logging system lifecycle](/doc/how_to/log_test.md)) into the
;; cache — `di/->memoize` takes the same middleware values
;; `di/start` does.

(t/deftest ownership-test
  (let [log     (atom [])
        logging (di/log :after-build!    #(swap! log conj [:built   (:key %)])
                        :after-demolish! #(swap! log conj [:stopped (:key %)]))
        mem     (di/->memoize {"DB_URL" "jdbc:test"} logging)]
    (-> (di/start `db mem)
        (di/stop))
    ;; the test's system was stopped, but the cached db was not
    (t/is (= [[:built "DB_URL"]
              [:built `db]]
             @log))
    (swap! log empty)

    (di/stop mem)
    ;; stopping mem released everything it cached, in reverse order
    (t/is (= [[:stopped `db]
              [:stopped "DB_URL"]]
             @log))))

;; A stub passed through an override deserves attention. Ownership
;; follows from who answers the key. A value in the override map is
;; answered by the map itself, outside the cache. But `di/ref` only
;; redirects the key to another one, and the referenced key is
;; answered by mem. Such a stub is cached and owned by mem like any
;; other component: tests that override `db` the same way share one
;; stub instance, and its stop hook runs at `(di/stop mem)`, not
;; when the test's system stops.

(defn stub-db
  {::di/kind :component
   ::di/stop (fn [db] (swap! (:log db) conj :stub-db-stopped))}
  [{log ::log}]
  {:log log, :conn (Object.)})

(t/deftest stub-ownership-test
  (let [log (atom [])
        mem (di/->memoize {::log log})]
    (-> (di/start `handler mem {`db (di/ref `stub-db)})
        (di/stop))
    ;; the test's system stopped, but the stub's stop hook did not run
    (t/is (= [] @log))

    (di/stop mem)
    ;; mem answered the `stub-db key, so mem owns the stub
    (t/is (= [:stub-db-stopped] @log))))

;; Keeping `with-open` around each test's system is still a good
;; habit: a test may build something outside the cache, and
;; stopping a fully cached system is cheap.

;; ## Editing a component between runs

;; The cache watches the vars it built components from. Redefine a
;; component — reload its namespace, re-evaluate its `defn` — and
;; the next start rebuilds it and everything that depends on it.
;; The rest of the cache stays. This is what makes the REPL loop
;; fast: edit a component, rerun its test, and only the affected
;; subgraph is rebuilt.

(t/deftest redefinition-test
  (with-open [mem (di/->memoize)]
    (defn router
      {::di/kind :component}
      []
      :v1)
    (with-open [system (di/start `router mem)]
      (t/is (= :v1 @system)))

    ;; edit and re-evaluate the component
    (defn router
      {::di/kind :component}
      []
      :v2)
    ;; the next start picks it up, no restart of mem needed
    (with-open [system (di/start `router mem)]
      (t/is (= :v2 @system)))))

;; ## One cache for the whole suite

;; The tests above create a fresh cache each to stay independent.
;; In a project, one memoized registry serves the whole suite,
;; held in a var. Fold everything the tests share into it: the
;; application registries, stubs for the clock and randomness,
;; test credentials. Per-test variation stays outside, as
;; overrides on `di/start`.

;; The system namespace owns the cache alongside the application
;; registry. For a CI run, the test-runner entry point wraps the
;; real runner: the cache is created before the first test and
;; released after the last one, even when the run throws. For the
;; REPL there is a pair of start and stop functions that bind the
;; same var.

;; ```clojure
;; (ns app.system
;;   (:require
;;    [clojure.edn :as edn]
;;    [darkleaf.di.core :as di]))
;;
;; ;; the application registry, shared with prod and dev
;; (defn registry []
;;   [...])
;;
;; (defn test-registries []
;;   [(registry)
;;    {:app/clock (di/ref `fixed-clock)}
;;    (-> "env_test.edn" slurp edn/read-string)])
;;
;; (declare mem)
;;
;; ;; the entry point for `clojure -X:test`
;; (defn wrap-test-runner [{:keys [runner] :as args}]
;;   (let [runner (requiring-resolve runner)]
;;     (with-open [m (di/->memoize (test-registries))]
;;       (def mem m)
;;       (runner args))))
;;
;; ;; the REPL counterpart
;; (defn start-test [] (def mem (di/->memoize (test-registries))))
;; (defn stop-test  [] (di/stop mem))
;; ```

;; The `:test` alias in `deps.edn` points at the wrapper and
;; passes the real runner as an argument:

;; ```clojure
;; :test {:extra-paths ["test"]
;;        :extra-deps  {io.github.cognitect-labs/test-runner
;;                      {:git/tag "v0.5.1" :git/sha "dfb30dd"}}
;;        :exec-fn     app.system/wrap-test-runner
;;        :exec-args   {:runner cognitect.test-runner.api/test}}
;; ```

;; Tests reach the cache through the var:

;; ```clojure
;; (ns app.login-test
;;   (:require
;;    [app.system :refer [mem]]
;;    ...))
;;
;; (t/deftest login-test
;;   (with-open [system (di/start `app.web/handler mem
;;                                {"SESSION_TTL" "60"})]
;;     ...))
;; ```

;; The same registry then serves interactive work: start a
;; subsystem, call it by hand, stop it — the expensive components
;; remain running for the next start. The
;; [`di/->memoize`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.core#->memoize)
;; docstring shows the matching workflow for a dev system.
