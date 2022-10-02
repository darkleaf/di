(ns example.adapters.flyway
  (:require
   [example.adapters.hikari :as-alias hikari])
  (:import
   (org.flywaydb.core Flyway)))

(defn migrate [{ds `hikari/datasource}]
  (.. (Flyway/configure)
      (dataSource ds)
      load
      migrate))
