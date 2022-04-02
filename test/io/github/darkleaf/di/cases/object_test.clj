(ns io.github.darkleaf.di.cases.object-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

(defn- logging []
  (let [p   (promise)
        log #(deliver p ::logged)]
    [p log]))


(defn stoppable-object [{log `log}]
  (reify di/Stoppable
    (stop [_]
      (log))))

(t/deftest stoppable-object-test
  (t/testing `di/stop
    (let [[p log] (logging)
          obj     (di/start `stoppable-object [{`log log} di/ns-registry])]
      (di/stop obj)
      (t/is (= ::logged @p))))
  (t/testing `with-open
    (let [[p log] (logging)]
      (with-open [obj (di/start `stoppable-object [{`log log} di/ns-registry])])
      (t/is (= ::logged @p)))))
