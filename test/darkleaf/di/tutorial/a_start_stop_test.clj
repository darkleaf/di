(ns darkleaf.di.tutorial.a-start-stop-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(def a ::a)

(t/deftest a-test
  (let [root (di/start `a)]
    (t/is (= ::a @root))
    (di/stop root)))

;; корень implements AutoCloseable

;; Instead of using `di/stop` in tests, you should use `with-open` to stop correctly.

;; идиома (with-open [root (di/start ...

(t/deftest a'-test
  (with-open [root (di/start `a)]
    (t/is (= ::a @root))))


(defn b []
  ::b)

(t/deftest b-test
  (with-open [root (di/start `b)]
    (t/is (= ::b @root))))

;; для объявления зависимостей используется map destructuring
;; разберемся с зависимостями позже

(defn c [-deps]
  ::c)

(t/deftest c-test
  (with-open [root (di/start `c)]
    (t/is (= ::c @root))))


(defn d [-deps]
  (fn []
    ::d))

(t/deftest d-test
  (with-open [root (di/start `d)]
    ;; we don't have to deref root to call it
    (t/is (= ::d (@root) (root)))))


;; DI provides more convenient way to define services.
;; Instead of using higher order function
;; just write a function with deps and its arguments.

(defn e [-deps arg]
  [::e arg])

(t/deftest e-test
  (with-open [root (di/start `e)]
    (t/is (= [::e 42] (root 42)))))


;; You don't need to restart the whole system if you redefine a service.
;; It's very helpful for interactive development.

;; In this test I use a dynamic var but
;; during development you would redefine a static one.

;; Это не работает, если добавляются новые зависимости.
;; В этом случае требуется перезапуск всей системы.


(defn ^:dynamic f [-deps arg]
  [::f arg])

(t/deftest f-test
  (with-open [root (di/start `f)]
    (binding [f (fn [-deps arg] [::new-f arg])]
      (t/is (= [::new-f 42] (root 42))))))
