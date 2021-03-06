(ns darkleaf.di.tutorial.p-functional-refs-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [clojure.spec.alpha :as s]))

;; In some cases, your components may have a complex structure or require transfromation.
;; You can use `di/ref` to transform a component.

;; The hard way

(defn port [{port "PORT"}]
  (Long/parseLong port))

(t/deftest port-test
  (with-open [system-root (di/start `port {"PORT" "8080"})]
    (t/is (= 8080 @system-root))))


;; The easy way

(def port* (di/ref "PORT" #(Long/parseLong %)))

(t/deftest port-test
  (with-open [system-root (di/start `port* {"PORT" "8080"})]
    (t/is (= 8080 @system-root))))


;; You can also use a ref to test an abstraction.
;; Also consider `di/with-decorator`.

(s/check-asserts true)

(s/def ::datasource ifn?)
(def datasource (di/ref ::datasource #(s/assert ::datasource %)))

(defn handler [{ds `datasource} -arg]
  :ok)

(t/deftest handler-test
  (t/is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"\ASpec assertion failed.*"
                          (di/start `handler {::datasource "wrong"})))
  (with-open [system-root (di/start `handler {::datasource (fn [])})]
    (t/is (= :ok (system-root :arg)))))
