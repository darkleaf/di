(ns darkleaf.di.tutorial.g-convenient-service-definition-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; DI provides more convenient way to define services.
;; Instead of using higher order function
;; just write a function with deps and its arguments.

;; In this example we do not use deps for handlers,
;; but ones may depend on database connections, etc.

(defn root-handler [-deps -req]
  {:status 200 :body "Hi!"})

(defn not-found-handler [-deps -req]
  {:status 404 :body "Not found :("})

(defn ring-handler [{root-handler      `root-handler
                     not-found-handler `not-found-handler} req]
  (case (:uri req)
    "/" (root-handler req)
    (not-found-handler req)))

(t/deftest start-ring-handler
  (let [system-root (di/start `ring-handler)]
    (t/is (= {:status 200 :body "Hi!"}          (system-root {:uri "/"})))
    (t/is (= {:status 404 :body "Not found :("} (system-root {:uri "/news"})))))
