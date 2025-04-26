;; # Intro

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.a-intro-test
  {:nextjournal.clerk/visibility {:result :hide}
   :nextjournal.clerk/toc        true}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (java.time Instant)))

;; Let's start.
;; In this chapter I'll show you how to deal with compoonents.

;; ## Trivial system

;; The following test describes the most trivial system that
;; contains the most trivial component.

(def a ::a)

;; `root` is a system root.
;; To get root's value deref it.
;; To stop a system use `di/stop`.

(t/deftest a-test
  (let [root (di/start `a)]
    (t/is (= ::a @root))
    (di/stop root)))

;; ## AutoCloseable

;; A root implements `AutoCloseable`
;; so in tests we should use `with-open` macro
;; for propperly stopping.

(t/deftest a'-test
  (with-open [root (di/start `a)]
    (t/is (= ::a @root))))

;; ## Component

;; A component definition is a function of 0 or 1 arity
;; with `{::di/kind :componnent}` meta.

(defn b
  {::di/kind :component}
  []
  (Instant/now))

(t/deftest b-test
  (with-open [root (di/start `b)]
    (t/is (inst? @root))))

;; ## Dependencies

;; To define a component dependes from other components
;; define a function of one argument.
;; DI will parse associative destructuing to get dependencides of the component.
;; We'll condider compoenent dependencies in the next chapter.
;; But now we will use placeholder.

(defn c
  {::di/kind :component}
  [-deps]
  ::c)

(t/deftest c-test
  (with-open [root (di/start `c)]
    (t/is (= ::c @root))))

;; ## Services

;; A service is a function with or without dependencies.

(defn d []
  ::d)

;; `root` is a wrapper, and it implements `clojure.lang.IFn`, just like `clojure.lang.Var`.
;; So you can just call `root`.

(t/deftest d-test
  (with-open [root (di/start `d)]
    (t/is (= ::d (@root) (root)))))

(defn d* [-deps]
  ::d)

(t/deftest d*-test
  (with-open [root (di/start `d*)]
    (t/is (= ::d (@root) (root)))))

(defn e [-deps arg]
  [::e arg])

(t/deftest e-test
  (with-open [root (di/start `e)]
    (t/is (= [::e 42] (root 42)))))

;; ## Interactive Development

;; You don't need to restart the whole system if you redefine a service.
;; Just redefine a Var.
;; It's very helpful for interactive development.

;; It does not work if you change definition of dependencies,
;; so in this case you have to restart the system.

;; The new implementation of a service will receive the same dependencies.
;; To check that, I have to look a little ahead and define component with a dependency.
;; As I said we consider deps in the next chapter.

(t/deftest f-test
  (defn f [{x ::x} arg]
    [::f x arg])
  (with-open [root (di/start `f {::x :x})]
    (defn f [deps arg]
      [::new-f (deps ::x) arg])
    (t/is (= [::new-f :x 42] (root 42)))))
