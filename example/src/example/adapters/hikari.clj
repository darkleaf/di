(ns example.adapters.hikari
  (:require
   [hikari-cp.core :as hikari]
   [io.github.darkleaf.di.core :as di])
  (:import
   [com.zaxxer.hikari HikariDataSource]))

(defn datasource [{options ::options}]
  (hikari/make-datasource options))

(extend-type HikariDataSource
  di/Stoppable
  (stop [this]
    (hikari/close-datasource this)))
