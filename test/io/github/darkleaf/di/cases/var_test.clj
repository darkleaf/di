(ns io.github.darkleaf.di.cases.var-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

;; A var can contain an implementation of `di/Factory` or have been defined with `defn`.

;; An object is a trivial factory itself.
(def value 42)

(t/deftest value-test
  (with-open [obj (di/start `value)]
    (t/is (= 42 @obj))))


;; A var itself implements `di/Factory`.
(def link #'value)

(t/deftest link-test
  (with-open [obj (di/start `link)]
    (t/is (= 42 @obj))))


;; port implements `di/Factory`
(def port (di/ref "PORT" #(Long/parseLong %)))

(t/deftest port-test
  (with-open [p (di/start `port
                          {"PORT" "8080"})]
    (t/is (= 8080 @p))))


;; A defn without arguments is not allowed.
(defn no-arg-fn []
  42)

(t/deftest no-arg-fn-test
  (t/is (thrown? ExceptionInfo (di/start `no-arg-fn))))


;; But you can explicitly define the function.
(def no-arg-fn* (fn [] 42))

(t/deftest no-arg-fn*-test
  (with-open [f (di/start `no-arg-fn*)]
    (t/is (= 42 (f)))))


;; A defn with one argument is an object constructor.
(defn object-constructor [{init ::init}]
  (atom init))

(t/deftest object-constructor-test
  (with-open [obj (di/start `object-constructor
                            {::init 42})]
    (t/is (= 42 @@obj))))


;; A defn with number of arguments is a service.
(defn service [{} arg]
  [::result arg])

(t/deftest service-test
  (with-open [s (di/start `service)]
    (t/is (= [::result 42] (s 42)))))


;; Defaults work as usual
(defn constructor-with-defaults [{qkey  ::param
                                  skey  :param
                                  ssym  'param
                                  qsym  `param
                                  qsym* 'undefined-ns/param
                                  str   "param"
                                  :or   {qkey  :qkey
                                         skey  :skey
                                         qsym  :qsym
                                         qsym* :qsym*
                                         ssym  :ssym
                                         str   :str}}]
  [qkey skey qsym qsym* ssym str])

(t/deftest constructor-with-defaults-test
  (with-open [obj (di/start `constructor-with-defaults)]
    (t/is (= [:qkey :skey :qsym :qsym* :ssym :str] @obj))))


;; DI uses all arities to detect dependencies.
(defn multi-arity-service
  ([]
   (multi-arity-service {`a :default-a, `b :default-b}))
  ([{a `a, :as deps}]
   (multi-arity-service deps :a1))
  ([{b `b, :as deps} arg]
   (multi-arity-service deps arg :a2))
  ([deps arg1 arg2]
   [::result deps arg1 arg2]))

(t/deftest multi-arity-service-test
  (t/is (= [::result {`a :default-a, `b :default-b} :a1 :a2]
           (multi-arity-service)))
  (with-open [s (di/start `multi-arity-service
                          {`a :a
                           `b :b})]
    (t/is (= [::result {`a :a, `b :b} :a1   :a2]   (s)))
    (t/is (= [::result {`a :a, `b :b} :arg1 :a2]   (s :arg1)))
    (t/is (= [::result {`a :a, `b :b} :arg1 :arg2] (s :arg1 :arg2)))))
