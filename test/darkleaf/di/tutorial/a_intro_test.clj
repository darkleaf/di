;; # Intro

^{::clerk/visibility {:code :hide}}
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

;; ## Constructor

;; If you want to perform some side effect
;; just define a function, and DI will call it to buils a component.

(defn b []
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

(defn c [-deps]
  ::c)

(t/deftest c-test
  (with-open [root (di/start `c)]
    (t/is (= ::c @root))))

;; ## Functions

;; Functions a first class objects so we can build one.

(defn d [-deps]
  (fn []
    ::d))

;; `root` is a wrapper, and it implements `clojure.lang.IFn`, just like `clojure.lang.Var`.
;; So you can just call `root`.

(t/deftest d-test
  (with-open [root (di/start `d)]
    (t/is (= ::d (@root) (root)))))

;; I will call the components that are functions services.

;; ## Services

;; DI provides more convenient way to define services.
;; Instead of using higher order functions
;; just write a function with deps and its arguments.

(defn e [-deps arg]
  [::e arg])

(t/deftest e-test
  (with-open [root (di/start `e)]
    (t/is (= [::e 42] (root 42)))))

;; ## REPL

;; You don't need to restart the whole system if you redefine a service.
;; Just redefine a Var.
;; It's very helpful for interactive development.

;; It does not work if you change definition of dependencies,
;; so in this case you have to restart the system.

;; The new implementation of a service will receive the same dependencies.
;; To check that, I have to look a little ahead and define component with a dependency.
;; As I said we consider deps in the next chapter.

;; I have to use a dynamic var to test this behaviour but
;; in real life during development you would redefine a static one.

(defn ^:dynamic f [{x ::x} arg]
  [::f x arg])

(t/deftest f-test
  (with-open [root (di/start `f {::x :x})]
    (binding [f (fn [deps arg]
                  [::new-f (deps ::x) arg])]
      (t/is (= [::new-f :x 42] (root 42))))))
