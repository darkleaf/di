(ns example.adapters.flyway
  (:require
   [darkleaf.di.core :as-alias di]
   [example.adapters.hikari :as-alias hikari])
  (:import
   (org.flywaydb.core Flyway)))

(defn migrate
  {::di/kind :component}
  [{ds `hikari/datasource}]
  (.. (Flyway/configure)
      (dataSource ds)
      load
      migrate))
