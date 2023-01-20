;; # Two databases

^{::clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.z-two-databases-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.string :as str]
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

;; In DI, each key corresponds to one object. So if you want to use two databases
;; you have to define two keys.

(defn db-factory [db-name]
  (let [db-name      (-> db-name name str/upper-case)
        url-key      (str "DB_" db-name "_URL")
        user-key     (str "DB_" db-name "_USER")
        password-key (str "DB_" db-name "_PASSWORD")]
    (reify p/Factory
      (dependencies [_]
        {url-key      :required
         user-key     :required
         password-key :required})
      (build [_ deps]
        [::db (deps url-key) (deps user-key) (deps password-key)]))))

(def db-a (db-factory :a))
(def db-b (db-factory :b))

(defn root [{db-a `db-a
             db-b `db-b}]
  [db-a db-b])

(t/deftest root-test
  (with-open [root (di/start `root {"DB_A_URL"      "tcp://a"
                                    "DB_A_USER"     "user_a"
                                    "DB_A_PASSWORD" "secret"
                                    "DB_B_URL"      "tcp://b"
                                    "DB_B_USER"     "user_b"
                                    "DB_B_PASSWORD" "super-secret"})]
    (t/is (= [[::db "tcp://a" "user_a" "secret"]
              [::db "tcp://b" "user_b" "super-secret"]]
             @root))))
