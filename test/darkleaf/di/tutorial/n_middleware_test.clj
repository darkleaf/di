(ns darkleaf.di.tutorial.n-middleware-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; Actually, the rest of the `di/start` arguments are middlewares.
;; Maps and lists are special cases of ones.
;; They a usefull for logging, schema validation, AOP, etc.

;; For this case we have `di/with-decorator` middleware.
;; https://en.wikipedia.org/wiki/Decorator_pattern

(defmulti check-schema! (fn [object key] key))

(defmethod check-schema! :default [_ _] nil)

(defn with-schema [object key]
  (check-schema! object key)
  object)

(defmethod check-schema! ::datasource [ds _]
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
