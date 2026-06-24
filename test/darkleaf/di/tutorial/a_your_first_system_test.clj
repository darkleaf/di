;; # Your first system

(ns darkleaf.di.tutorial.a-your-first-system-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (java.time Instant)))

;; By the end of this chapter you can start a small system, stop
;; it, and see the difference between a component and a service.
;; The rest of the tutorial builds on these terms.


;; ## The smallest system

;; A system is one or more components connected by their
;; dependencies. The smallest one has a single component — a
;; trivial value stored in a var.

(def a ::a)

;; `di/start` builds the system. It takes a key, looks it up, and
;; returns the system root. Deref the root with `@` to get the
;; built value. `di/stop` shuts the system down.

(t/deftest a-test
  (let [root (di/start `a)]
    (t/is (= ::a @root))
    (di/stop root)))

;; ## Stopping safely

;; The root implements `AutoCloseable`, so in tests use `with-open`
;; instead of calling `di/stop` by hand. If a test fails midway,
;; the system is still stopped.

(t/deftest a'-test
  (with-open [root (di/start `a)]
    (t/is (= ::a @root))))

;; ## Components

;; A component is a function of zero or one argument with
;; `{::di/kind :component}` metadata. DI calls it once during start
;; and uses the returned value.

(defn b
  {::di/kind :component}
  []
  (Instant/now))

(t/deftest b-test
  (with-open [root (di/start `b)]
    (t/is (inst? @root))))

;; The argument, when present, carries the component's
;; dependencies. DI reads the destructuring map to figure out what
;; to inject. Declaring real dependencies is the next chapter; for
;; now we just use a placeholder name.

(defn c
  {::di/kind :component}
  [-deps]
  ::c)

(t/deftest c-test
  (with-open [root (di/start `c)]
    (t/is (= ::c @root))))

;; ## Services

;; A service is a plain `defn` — no metadata. DI does not call it
;; during start; the function itself is the component.

(defn d []
  ::d)

;; The root implements `clojure.lang.IFn`, so you can call it
;; directly — `(root)` invokes the underlying function, just like
;; you would invoke a var.

(t/deftest d-test
  (with-open [root (di/start `d)]
    (t/is (= ::d (@root) (root)))))

;; A service can also take dependencies. As with a component, the
;; first argument is the dependency map (placeholder for now).

(defn d* [-deps]
  ::d)

(t/deftest d*-test
  (with-open [root (di/start `d*)]
    (t/is (= ::d (@root) (root)))))

;; Arguments after the dependency map are the service's own
;; arguments, supplied by the caller.

(defn e [-deps arg]
  [::e arg])

(t/deftest e-test
  (with-open [root (di/start `e)]
    (t/is (= [::e 42] (root 42)))))

;; That's the vocabulary: system, root, components, and services.
;; The next chapter wires components together through real
;; dependencies.

