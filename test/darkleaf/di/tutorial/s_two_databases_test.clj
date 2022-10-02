(ns darkleaf.di.tutorial.s-two-databases-test
  (:require
   [clojure.string :as str]
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

;; In DI, each key corresponds to one object. So if you want to use two databases
;; you have to define two keys.

;; здесь можно фабрику использовать, чтобы не дублировать список зависимостей

(defn db-factory [prefix]
  (let [prefix       (-> prefix name str/upper-case)
        url-key      (str prefix "_DB_URL")
        user-key     (str prefix "_DB_USER")
        password-key (str prefix "_DB_PASSWORD")]
    (reify p/Factory
      (dependencies [_]
        {url-key      :required
         user-key     :required
         password-key :required})
      (build [_ deps]
        [::db (deps url-key) (deps user-key) (deps password-key)]))))

(def main-db (db-factory :main))
(def secondary-db (db-factory :secondary))

(defn handler [{main-db      `main-db
                secondary-db `secondary-db} -req]
  [main-db secondary-db])

(t/deftest handler-test
  (with-open [root (di/start `handler {"MAIN_DB_URL"           "tcp://main"
                                       "MAIN_DB_USER"          "main"
                                       "MAIN_DB_PASSWORD"      "secret"
                                       "SECONDARY_DB_URL"      "tcp://secondary"
                                       "SECONDARY_DB_USER"     "secondary"
                                       "SECONDARY_DB_PASSWORD" "super-secret"})]
    (t/is (= [[::db "tcp://main" "main" "secret"]
              [::db "tcp://secondary" "secondary" "super-secret"]]
             (root :unused-req)))))
