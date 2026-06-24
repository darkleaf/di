;; # Abstractions

(ns darkleaf.di.tutorial.f-abstractions-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.utils :as u]))

;; Symbol keys resolve to vars — when DI sees `` `foo `` in a
;; destructuring map, it looks up the var `foo` in the current
;; namespace. That's the default and it covers most code: you are
;; wiring functions together by their names.

;; Sometimes the dependency does not belong to any one namespace —
;; a database connection, a session source, a config map. There
;; is no var to point at; the value comes from outside the code.
;; For these, use a keyword key. DI does not try to resolve it;
;; the registry must supply it.

;; ## A worked example

;; In the example below, `::datasource` and `::session` are
;; abstractions — they have no vars. The `ring-handler` does not
;; care where they come from. At start, the registry binds them.

(defn get-user [{ds ::datasource} id]
  (ds id))

(defn get-current-user [{session  ::session
                         get-user `get-user}]
  (-> session :user-id get-user))

(defn ring-handler [{get-current-user `get-current-user} -req]
  {:status 200 :body (str "Hi, " (get-current-user) "!")})

(t/deftest handler-test
  (with-open [root (di/start `ring-handler
                             {::datasource {1 "John"}
                              ::session    {:user-id 1}})]
    (t/is (= {:status 200 :body "Hi, John!"}
             (root {})))))

;; Notice the mix: `get-user` and `get-current-user` are symbol
;; deps (they have real vars). `::datasource` and `::session` are
;; keyword deps (no vars; supplied at start). Both kinds sit side
;; by side in the same destructuring map.

;; ## When to pick which

;; **Symbol** points at a specific var. You are naming the
;; implementation directly. This is the default for code you
;; write — no extra wiring needed; DI resolves the var.

;; **Keyword** does not point at anything. It names an abstract
;; role; the registry decides which implementation fills it. Use
;; a keyword when you have explicitly decided to abstract a
;; dependency — most commonly in a reusable library or an
;; internal module that declares what it needs without naming
;; the implementation, or when the role itself has no canonical
;; default that should live in code.

;; Both kinds can be overridden by a registry — swap-ability is
;; not the deciding factor. The choice is whether the dependency
;; has a default implementation tied to a var, or is abstract by
;; design.

;; ## Validating the abstraction

;; Because a keyword has no var, the registry can supply anything
;; for it. If you want a contract check — must be callable, must
;; satisfy a protocol, must match a spec — declare a same-named
;; component that validates the registry value and returns it.
;; Downstream code depends on the wrapper symbol, not the raw
;; keyword.

(defn user-repo
  {::di/kind :component}
  [{repo ::user-repo}]
  (when-not (ifn? repo)
    (throw (ex-info "::user-repo must be callable" {:value repo})))
  repo)

;; Good value goes through:

(t/deftest checked-user-repo-test
  (with-open [root (di/start `user-repo
                             {::user-repo #(str "user-" %)})]
    (t/is (= "user-7" (@root 7)))))

;; A bad value fails the system at start, before any code touches
;; it. The original error is wrapped as `::di/build-failure`; the
;; original is available via `ex-cause`.

(t/deftest checked-user-repo-failure-test
  (let [ex (u/catch-some (di/start `user-repo {::user-repo 42}))]
    (t/is (= ::di/build-failure (-> ex ex-data :type)))
    (t/is (= "::user-repo must be callable"
             (-> ex ex-cause ex-message)))
    (t/is (= {:value 42}
             (-> ex ex-cause ex-data)))))

;; The next chapter introduces the third kind of key: strings,
;; for pulling in environment variables.
