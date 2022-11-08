(ns example.adapters.jetty
  (:require
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as di.p]
   [ring.adapter.jetty :as jetty])
  (:import
   [org.eclipse.jetty.server Server]))

(defn server [{handler ::handler
               options ::options}]
  (jetty/run-jetty handler (assoc options :join? false)))

(extend-type Server
  di.p/Stoppable
  (stop [this]
    (.stop this)))
