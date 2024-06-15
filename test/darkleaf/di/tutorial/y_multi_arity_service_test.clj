;; # Multi arity service

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.y-multi-arity-service-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; DI collects dependencies from all arities and only then resolves dependencies.

(defn multi-arity-service
  ([{a `a, :as deps}]
   (multi-arity-service deps :a1))
  ([{b `b, :as deps} arg]
   (multi-arity-service deps arg :a2))
  ([deps arg1 arg2]
   [::result deps arg1 arg2]))

(t/deftest multi-arity-service-test
  (with-open [s (di/start `multi-arity-service {`a :a, `b :b})]
    ;; each arity gets all the dependencies
    (t/is (= [::result {`a :a, `b :b} :a1   :a2]   (s)))
    (t/is (= [::result {`a :a, `b :b} :arg1 :a2]   (s :arg1)))
    (t/is (= [::result {`a :a, `b :b} :arg1 :arg2] (s :arg1 :arg2)))))

;; other


(comment
  (defn ^::di/service xxx-service-0 []
    (gensym))

  ;; or?
  (defn xxx-service-0
    {::di/kind :service}
    []
    (gensym))

  ,,,)


(defn ^::di/service xxx-service-0 []
  (gensym))

(t/deftest xxx-service-0-test
  (with-open [s (di/start `xxx-service-0)]
    (t/is (not= (s) (s))))

  ,,)


(defn ^::di/service xxx-service-1 [-deps]
  (gensym))

(t/deftest xxx-service-1-test
  (with-open [s (di/start `xxx-service-1)]
    (t/is (not= (s) (s))))

  ,,)


(defn ^::di/service xxx-service-0-1
  ([]
   0)
  ([-deps]
   1))

(t/deftest xxx-service-0-1-test
  (with-open [s (di/start `xxx-service-0-1)]
    (t/is (= 1 (s))))
  ,,)
