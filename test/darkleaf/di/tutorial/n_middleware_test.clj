(ns darkleaf.di.tutorial.n-middleware-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Actually, the rest of the `di/start` arguments are middlewares.
;; Maps and lists are special cases of ones.
;; They a usefull for logging, schema validation, AOP, etc.

;; For this case we have `di/with-decorator` middleware.
;; https://en.wikipedia.org/wiki/Decorator_pattern

(defmulti check-schema! (fn [key object] key))

(defmethod check-schema! :default [_ object])

(defn with-schema [key object]
  (check-schema! key object)
  object)


(defmethod check-schema! ::datasource [_ ds]
  (assert (ifn? ds)))

(defn service [{ds ::datasource} x]
  [:service (ds x)])

(t/deftest service-test
  (with-open [system-root (di/start `service
                                    {::datasource {:x 42}}
                                    (di/wrap with-schema))]
    (t/is (= [:service 42] (system-root :x)))))

(t/deftest schema-test
  (t/is (thrown? AssertionError
                 (di/start `service
                           {::datasource "wrong"}
                           (di/wrap with-schema)))))
