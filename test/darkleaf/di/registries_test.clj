(ns darkleaf.di.registries-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

(def dep 'dep)

(t/deftest default-registries-test
  (with-open [obj (di/start `dep)]
    (t/is (= 'dep @obj)))
  (with-open [obj (di/start "PATH")]
    (t/is (= (System/getenv "PATH") @obj))))

(t/deftest undefined-test
  (t/is (thrown? Throwable (di/start `undefined)))
  (t/is (thrown? Throwable (di/start "DI_UNDEFINED"))))

(t/deftest map-registry-test
  (with-open [obj (di/start `dep {`dep :stub})]
    (t/is (= :stub @obj))))

(t/deftest priority-test
  (with-open [obj (di/start `dep)]
    (t/is (= dep @obj)))
  (with-open [obj (di/start `dep {`dep :stub})]
    (t/is (= :stub @obj))))

(t/deftest vector-registry-test
  (let [reg (fn [default]
              (fn [super]
                (fn [key]
                   (if-some [factory (super key)]
                     factory
                     default))))]
    (with-open [obj (di/start `not-found (reg :default))]
      (t/is (= :default @obj)))))

(t/deftest sequential-registry-test
  (let [registries [{::a 1}
                    {::b 2}]]
    (with-open [obj (di/start ::a registries)]
      (t/is (= 1 @obj)))
    (with-open [obj (di/start ::b registries)]
      (t/is (= 2 @obj)))))

(defn service [-deps x]
  [:service x])

(defn instrument-service [obj]
  (fn [x]
    (assert (int? x))
    (obj x)))

(t/deftest transform-test
  (with-open [obj (di/start `service
                            (di/transform `service instrument-service))]
    (t/is (= [:service 42] (obj 42)))
    (t/is (thrown? Throwable (obj "42")))))
