(ns example.adapters.jetty
  (:require
   [example.adapters.reitit :as-alias reitit]
   [io.github.darkleaf.di.core :as di]
   [ring.adapter.jetty :as jetty])
  (:import
   [org.eclipse.jetty.server Server]))

(defn server [{handler `reitit/handler
               opts    ::options}]
  (jetty/run-jetty handler (assoc opts :join? false)))

(extend-type Server
  di/Stoppable
  (stop [this]
    (.stop this)))
