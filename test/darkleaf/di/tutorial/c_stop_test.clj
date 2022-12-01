(ns darkleaf.di.tutorial.c-stop-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

;; To stop a component, you should teach DI how to do it.
;; Use `::di/stop` to define a stop function.

(defn root
  {::di/stop #(reset! % true)}
  [{::keys [*stopped?]}]
  *stopped?)

(t/deftest stop-test
  (let [*stopped? (atom false)]
    (with-open [root (di/start `root {::*stopped? *stopped?})]
      (t/is (= false @@root)))
    (t/is @*stopped?)))

;; todo: Or p/Stoppable
