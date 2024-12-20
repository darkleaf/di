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

(defn nil-component
  {::di/kind :component}
  []
  nil)

(defn nil-component-1-arity
  {::di/kind :component}
  [_]
  nil)

(t/deftest nil-component-test
  (t/is (= [{:stack [`nil-component ::di/implicit-root]}
            {:type ::di/nil-component}]
           (catch-cause-data (di/start `nil-component))))
  (t/is (= [{:stack [`nil-component-1-arity ::di/implicit-root]}
            {:type ::di/nil-component}]
           (catch-cause-data (di/start `nil-component-1-arity)))))
