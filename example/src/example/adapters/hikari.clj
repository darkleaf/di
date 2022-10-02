(ns example.adapters.hikari
  (:require
   [hikari-cp.core :as hikari]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as di.p])
  (:import
   (com.zaxxer.hikari HikariDataSource)))

(defn datasource [{options ::options}]
  (hikari/make-datasource options))

(extend-type HikariDataSource
  di.p/Stoppable
  (stop [this]
    (hikari/close-datasource this)))
