(ns darkleaf.di.tutorial.j-stopping-in-tests-test
  (:require [clojure.test :as t]
            [darkleaf.di.core :as di]))

(def ^:dynamic *log-stop*)

(defn db-connection []
  (reify di/Stoppable
    (stop [_]
      (*log-stop* :db-connection))))

(defn ring-handler [{db `db-connection} -req]
  {:status 200})

;; Instead of using `di/stop` in tests, you should use `with-open` to stop correctly.

(t/deftest with-open-test
  (let [stops (atom [])]
    (binding [*log-stop* #(swap! stops conj %)]
      (with-open [system-root (di/start `ring-handler)]
        (t/is (= {:status 200} (system-root {}))))
      (t/is (= [:db-connection] @stops)))))
