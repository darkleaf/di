(ns darkleaf.di.component-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

(defmacro catch-cause-data [& body]
  `(try ~@body
        (catch clojure.lang.ExceptionInfo e#
          (->> e#
               Throwable->map
               :via
               (mapv :data)))))

(defn nil-value-component
  {::di/kind :component}
  []
  nil)

(defn nil-value-component-1-arity
  {::di/kind :component}
  [_]
  nil)

(t/deftest nil-value-component-test
  (t/is (= [{:type  ::di/build-obj-fail
             :stack [`nil-value-component ::di/implicit-root]}
            {:type ::di/nil-value-component}]
           (catch-cause-data (di/start `nil-value-component))))
  (t/is (= [{:type  ::di/build-obj-fail
             :stack [`nil-value-component-1-arity ::di/implicit-root]}
            {:type ::di/nil-value-component}]
           (catch-cause-data (di/start `nil-value-component-1-arity)))))
