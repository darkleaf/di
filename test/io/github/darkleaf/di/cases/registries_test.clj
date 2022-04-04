(ns io.github.darkleaf.di.cases.registries-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

(def dep 'dep)

(t/deftest default-registries-test
  (with-open [obj (di/start `dep)]
    (t/is (= 'dep @obj)))
  (with-open [obj (di/start "PATH")]
    (t/is (= (System/getenv "PATH") @obj))))

(t/deftest nil-registry-test
  (t/is (thrown? Throwable (di/start `dep [])))
  (t/is (thrown? Throwable (di/start "PATH" []))))

(t/deftest ns-registry-test
  (with-open [obj (di/start `dep [di/ns-registry])]
    (t/is (= 'dep @obj))))

(t/deftest env-registry-test
  (with-open [obj (di/start "PATH" [di/env-registry])]
    (t/is (= (System/getenv "PATH") @obj))))

(t/deftest map-registry-test
  (with-open [obj (di/start `dep [{`dep :stub}])]
    (t/is (= :stub @obj))))

(t/deftest priority-test
  (with-open [obj (di/start `dep [di/ns-registry {`dep :stub}])]
    (t/is (= dep @obj)))
  (with-open [obj (di/start `dep [{`dep :stub} di/ns-registry])]
    (t/is (= :stub @obj))))

(t/deftest vector-registry-test
  (let [reg (fn [previous default]
              (fn [key]
                (if-some [factory (previous key)]
                  factory
                  default)))]
    (with-open [obj (di/start `not-found [di/ns-registry [reg :default]])]
      (t/is (= :default @obj)))))


(defn service [-deps x]
  [:service x])

(defn instrument-service [-deps key obj]
  (if (= `service key)
    (fn [x]
      (assert (int? x))
      (obj x))
    obj))

(t/deftest decorating-registry-test
  (with-open [obj (di/start `service
                            [di/ns-registry
                             [di/decorating-registry `instrument-service]])]
    (t/is (= [:service 42] (obj 42)))
    (t/is (thrown? Throwable (obj "42")))))
