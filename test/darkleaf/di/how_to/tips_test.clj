;; # Tips

;; A collection of small DI tricks and lesser-known features.

(ns darkleaf.di.how-to.tips-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; ## `::di/stop` implies `:component`

;; You don't need to attach `{::di/kind :component}` if the
;; function already has `::di/stop` metadata. DI treats any
;; function with a stop hook as a component — there is no other
;; reasonable interpretation, since services don't have a built
;; value to stop.

(defn resource
  {::di/stop #(reset! % :stopped)}
  []
  (atom :running))

(t/deftest stop-implies-component-test
  (let [root (di/start `resource)
        a    @root]
    (t/is (= :running @a))
    (di/stop root)
    (t/is (= :stopped @a))))

;; ## Pass a vector instead of `apply`

;; When your registries come from a helper that returns a
;; collection, you might reach for `(apply di/start ...)`. You do
;; not need to. `di/start` treats a seqable value as a single
;; argument, so pass the collection directly.

(t/deftest grouped-registry-test
  ;; registries from a helper — instead of:
  ;;   (apply di/start ::root registries)
  ;; pass them as one vector:
  (with-open [r (di/start ::root [{::root :first}
                                  {::root :replacement}])]
    (t/is (= :replacement @r))))

;; ## Conditional registry entries

;; A registry function can wrap each contribution in `when` and
;; return the vector unconditionally. A disabled entry becomes
;; `nil`, and `nil` is a valid middleware — a no-op
;; ([The middleware argument](/doc/reference/middleware_argument.md)).
;; So one vector holds any number of conditional entries. The
;; [Feature flags](/doc/how_to/feature_flags_test.md) recipe uses
;; this shape for subsystem registries.

(defn features-registry [{:keys [alpha-enabled beta-enabled]}]
  [(when alpha-enabled {::alpha :on})
   (when beta-enabled  {::beta  :on})
   {::gamma :on}])

(t/deftest conditional-entries-test
  (let [registry (features-registry {:alpha-enabled true
                                     :beta-enabled  false})]
    (with-open [root (di/start ::alpha registry)]
      (t/is (= :on @root)))))

;; ## A key does not need a `require`

;; A dependency is a key — a symbol, a keyword, a string. A key
;; is data: the reader reads it without loading any code. So a
;; component can depend on a var from a namespace that its own
;; namespace does not require:

;; ```clojure
;; (ns app.handlers ; app.db is not required
;;   (:require
;;    [darkleaf.di.core :as-alias di]))

;; (defn get-user
;;   {::di/kind :component}
;;   [{db 'app.db/db}]
;;   ...)
;; ```

;; DI loads `app.db` by itself: at start, a symbol key is
;; resolved with `requiring-resolve`. Most of the time you take
;; a single key from a namespace, and a `require` for one key is
;; not worth the ceremony — just write the key in full.

;; There is also a structural gain: namespaces stop depending on
;; each other at load time. Reloading gets lighter, and two
;; namespaces whose components use each other do not form a
;; require cycle.

;; ## An EDN file instead of dotenv

;; Development and tests need a dozen environment variables:
;; ports, database URLs, tokens. The usual answer is dotenv
;; tooling — a `.env` file plus a library or a shell hook that
;; loads it. With DI you don't need any of that: put the
;; variables in an EDN file and read it into the registry.

;; ```clojure
;; ;; env_dev.edn
;; {"PORT"           "8081"
;;  "DATABASE_URL"   "jdbc:postgresql://localhost/app"
;;  ;; comments work, and #_ turns an entry off:
;;  #_#_"BASIC_AUTH" "admin"}
;; ```

;; ```clojure
;; (di/start ::root
;;           [(base-registry)
;;            (-> "env_dev.edn" slurp edn/read-string)])
;; ```

;; This works because an environment variable is just a string
;; key, and a map is a middleware that overrides the keys it
;; lists — components can't tell the difference. The last
;; registry element is the outermost, so the file wins over both
;; the registries before it and the real environment; a key that
;; is not in the file falls through to `System/getenv` as usual
;; ([The middleware argument](/doc/reference/middleware_argument.md)).

;; Note that the values are strings, exactly what `System/getenv`
;; would return. The file is not a second configuration system —
;; it is another source for the same contract, so `di/env-parsing`
;; and `:or` defaults keep working unchanged.
