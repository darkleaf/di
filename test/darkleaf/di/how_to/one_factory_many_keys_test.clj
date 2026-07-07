;; # One factory, many keys

(ns darkleaf.di.how-to.one-factory-many-keys-test
  (:require
   [clojure.string :as str]
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

;; Each key names one component. When several components share the same
;; build logic and differ only by their parameters, write the factory once
;; as a function that returns a
;; [`Factory`](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT/api/darkleaf.di.protocols#Factory),
;; then bind its result to a separate key for each component.

;; The example connects to two databases. `db-factory` takes a database
;; name and returns a factory whose dependencies are the environment
;; variables for that database. `db-a` and `db-b` are two keys backed by
;; the same factory.

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
      (build [_ deps _]
        [::db (deps url-key) (deps user-key) (deps password-key)])
      (description [_]
        {}))))

(def db-a (db-factory :a))
(def db-b (db-factory :b))

(defn root
  {::di/kind :component}
  [{db-a `db-a
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
