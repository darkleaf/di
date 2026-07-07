;; # Multi-arity services

(ns darkleaf.di.how-to.multi-arity-service-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Clojure services often have multiple arities — one with defaults
;; that calls into a richer one. DI handles this by collecting
;; dependencies from every arity, then resolving them once before
;; the service is bound. Each arity receives the same fully-resolved
;; dependency map.

(defn multi-arity-service
  ([{a `a, :as deps}]
   (multi-arity-service deps :a1))
  ([{b `b, :as deps} arg]
   (multi-arity-service deps arg :a2))
  ([deps arg1 arg2]
   [::result deps arg1 arg2]))

;; The 1-arg arity declares `a`; the 2-arg arity declares `b`. DI
;; reads both and resolves both — when you call `(s)`, you still
;; get a map containing `a` and `b`.

(t/deftest multi-arity-service-test
  (with-open [s (di/start `multi-arity-service {`a :a, `b :b})]
    (t/is (= [::result {`a :a, `b :b} :a1   :a2]   (s)))
    (t/is (= [::result {`a :a, `b :b} :arg1 :a2]   (s :arg1)))
    (t/is (= [::result {`a :a, `b :b} :arg1 :arg2] (s :arg1 :arg2)))))
