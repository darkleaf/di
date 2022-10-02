(ns darkleaf.di.tutorial.c-stop-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

;; To stop a value, you should teach DI how to do it
;; through the `p/Stoppable` protocol implementation.

(defn root [{::keys [*stopped?]}]
  (reify p/Stoppable
    (stop [_]
      (reset! *stopped? true))))

(t/deftest stop-test
  (let [*stopped? (atom false)]
    (-> (di/start `root {::*stopped? *stopped?})
        (di/stop))
    (t/is @*stopped?)))


(comment
  (ns project.jetty
    (:require
     [ring.adapter.jetty :as jetty]
     [darkleaf.di.core :as di]
     [darkleaf.di.protocols :as di.p])
    (:import
     (org.eclipse.jetty.server Server)))

  (extend-type Server
    di.p/Stoppable
    (stop [this]
      (.stop this))))
