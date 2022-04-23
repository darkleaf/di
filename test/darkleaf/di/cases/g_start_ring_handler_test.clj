(ns darkleaf.di.cases.g-start-ring-handler-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;;; In this example we do not use deps,
;;; but your handler may depend on database connections, etc.
(defn ring-handler [-deps req]
  (case (:uri req)
    "/" {:status 200 :body "Hi!"}
    {:status 404 :body "Not found :("}))

(t/deftest start-ring-handler
  (let [system-root (di/start `ring-handler)]
    (t/is (= {:status 200 :body "Hi!"}          (system-root {:uri "/"})))
    (t/is (= {:status 404 :body "Not found :("} (system-root {:uri "/news"})))))
