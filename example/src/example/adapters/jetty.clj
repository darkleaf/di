(ns example.adapters.jetty
  (:require
   [darkleaf.di.core :as-alias di]
   [ring.adapter.jetty :as jetty]))

(defn server
  {::di/stop (memfn stop)}
  [{handler ::handler
    port    :env.long/PORT
    :or     {port 8080}}]
  (jetty/run-jetty handler {:join? false
                            :port  port}))
