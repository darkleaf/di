;; # Abstractions

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.m-abstractions-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; For some reasons, we may want to not depend on specific vars.
;; In this case, use keywords instead of symbols to define dependencies.
;; Later in the main function you will be able to bind all parts of your application.

(defn get-user [{ds ::datasource} id]
  (ds id))

(defn get-current-user [{session  ::session
                         get-user `get-user}]
  (fn []
    (-> session :user-id get-user)))

(defn ring-handler [{get-current-user `get-current-user} -req]
  {:status 200 :body (str "Hi, " (get-current-user) "!")})

(t/deftest handler-test
  (with-open [root (di/start `ring-handler
                             {::datasource {1 "John"}
                              ::session    {:user-id 1}})]
    (t/is (= {:status 200 :body "Hi, John!"}
             (root {})))))
