;; # All public vars as a component

(ns darkleaf.di.how-to.ns-publics-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; `di/ns-publics` treats every public var of a namespace as a
;; component or service and bundles them under
;; `:ns-publics/<ns-name>`. Starting that key gives you a map of
;; var name → built object — handy when you want a group of
;; related components and services without listing each one
;; explicitly.

;; Vars holding `nil` or unbound vars are skipped.

(def nil-component nil) ; excluded

(def unbound-component) ; excluded

(defn component
  {::di/kind :component}
  []
  :component)

(defn service [{component `component} arg]
  [component arg])

(t/deftest ok-test
  (with-open [system (di/start :ns-publics/darkleaf.di.how-to.ns-publics-test
                               (di/ns-publics))]
    (t/is (map? @system))
    (t/is (= #{:component :service :ok-test}
             (set (keys @system))))
    (t/is (= :component (:component system)))
    (t/is (= [:component :my-arg] ((:service system) :my-arg)))))

;; ## In practice

;; The feature originated in test infrastructure: a single global
;; test system was kept running, with adapter namespaces (a
;; database client, etc.) registered as roots via `ns-publics` so
;; any test could reach into them directly. `di/->memoize` later
;; covered the same ground with less ceremony — each test starts
;; just the keys it touches against a shared cache. `ns-publics`
;; still works as documented, but `->memoize` is the preferred
;; way to do that now.
