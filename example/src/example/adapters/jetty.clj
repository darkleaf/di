(ns example.adapters.jetty
  (:require
   [darkleaf.di.core :as-alias di]
   [ring.adapter.jetty :as jetty]))

(defn server
  {::di/stop #(.stop %)}
  [{handler ::handler
    options ::options}]
  (jetty/run-jetty handler (assoc options :join? false)))
