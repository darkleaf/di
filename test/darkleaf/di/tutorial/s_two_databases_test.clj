(ns darkleaf.di.tutorial.s-two-databases-test
  (:require [clojure.test :as t]
            [darkleaf.di.core :as di]))

;; In DI, each key corresponds to one object. So if you want to use two databases
;; you have to define two keys.

(defn datasource [url user password]
  [:ds-stub url user password])

(defn main-db [{url      "MAIN_DB_URL"
                user     "MAIN_DB_USER"
                password "MAIN_DB_PASSWORD"}]
  (datasource url user password))

(defn secondary-db [{url      "SECONDARY_DB_URL"
                     user     "SECONDARY_DB_USER"
                     password "SECONDARY_DB_PASSWORD"}]
  (datasource url user password))

(defn handler [{main-db      `main-db
                secondary-db `secondary-db} -req]
  [main-db secondary-db])

(t/deftest handler-test
  (with-open [system-root (di/start `handler {"MAIN_DB_URL"           "tcp://main"
                                              "MAIN_DB_USER"          "main"
                                              "MAIN_DB_PASSWORD"      "secret"
                                              "SECONDARY_DB_URL"      "tcp://secondary"
                                              "SECONDARY_DB_USER"     "secondary"
                                              "SECONDARY_DB_PASSWORD" "super-secret"})]
    (t/is (= [[:ds-stub "tcp://main" "main" "secret"]
              [:ds-stub "tcp://secondary" "secondary" "super-secret"]]
             (system-root :unused-req)))))
