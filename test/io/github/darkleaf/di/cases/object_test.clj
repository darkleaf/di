(ns io.github.darkleaf.di.cases.object-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

;; Functions with one argument are object constructors.
;; `{}` means the object has no deps.
(defn object [{}]
  ::new-object)

(t/deftest start-object-test
  (with-open [obj (di/start `object)]
    (t/is (= ::new-object @obj))))


(defn stoppable-object [{log `log}]
  (reify di/Stoppable
    (stop [_]
      (log))))

(t/deftest stoppable-object-test
  (let [p (promise)]
    (with-open [obj (di/start `stoppable-object {`log #(deliver p ::logged)})])
    (t/is (= ::logged @p)))
  (let [p   (promise)
        obj (di/start `stoppable-object {`log #(deliver p ::logged)})]
    (di/stop obj)
    (t/is (= ::logged @p))))


(defn auto-closeable-object [{log `log}]
  (reify java.lang.AutoCloseable
    (close [_]
      (log))))

(t/deftest auto-closeable-object-test
  (let [p (promise)]
    (with-open [obj (di/start `auto-closeable-object {`log #(deliver p ::logged)})])
    (t/is (= ::logged @p)))
  (let [p   (promise)
        obj (di/start `auto-closeable-object {`log #(deliver p ::logged)})]
    (di/stop obj)
    (t/is (= ::logged @p))))
