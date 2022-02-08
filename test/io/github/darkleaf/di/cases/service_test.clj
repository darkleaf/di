(ns io.github.darkleaf.di.cases.service-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

;; `{}` means an service has no deps.
(defn service [{} arg1 arg2]
  [::result arg1 arg2])

(t/deftest start-service-test
  (with-open [s (di/start `service)]
    ;; `s` implements IDeref
    (t/is (= [::result 1 2] (@s 1 2)))
    ;; `s` implements IFn
    (t/is (= [::result 1 2] (s 1 2)))))


;; We don't need to restart the whole system if we redefine a service.
;; It's very helpful for interactive development.
;; In the test I use dynamic var but
;; during development you could redefine the static var.

(defn ^:dynamic *service* [{} arg1]
  [::result arg1])

(t/deftest dynamic-service-test
  (with-open [s (di/start `*service*)]
    (binding [*service* (fn [{} arg1]
                          [::other-result arg1])]
      (t/is (= [::other-result 1] (s 1))))))


;; We can use an object like a service but we can't redefine it.

(defn object-fn [{}]
  (fn service [arg1]
    [::result arg1]))

(t/deftest object-fn-test
  (with-open [s (di/start ::object-fn)]
    (t/is (= [::result 1] (@s 1)))
    (t/is (= [::result 1] (s 1)))))


;; As the builder receives only dependencies,
;; we can use it to build both an object and
;; a no-args service.

(defn object-as-service [{}]
  (Math/random))

(t/deftest object-as-service-test
  (with-open [obj (di/start ::object-as-service)
              s   (di/start `object-as-service)]
    (t/is (double? @obj))
    (t/is (double? (s)))
    (t/is (identical? @obj @obj))
    (t/is (not= (s) (s)))
    (t/is (not= @obj (s)))))


;; DI uses last arity to detect dependencies.

(defn multi-arity-service
  ([deps] (multi-arity-service deps nil))
  ([{dep ::dep} arg] [::result dep arg]))

(t/deftest multi-arity-service-test
  (with-open [s (di/start `multi-arity-service {::dep 42})]
    (t/is (= [::result 42 nil] (s)))
    (t/is (= [::result 42 1] (s 1)))))


(defn var-arg-service [{} & args]
  [::result args])

(t/deftest var-arg-service-test
  (with-open [s (di/start `var-arg-service)]
    (t/is (= [::result [1 2 3 4]] (s 1 2 3 4)))))
