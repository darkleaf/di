(ns example.core
  (:require
   [darkleaf.di.core :as di]
   [example.adapters.hikari :as-alias hikari]
   [jsonista.core :as json]
   [next.jdbc :as jdbc]
   [ring.core.protocols :as r.proto]
   [ring.util.http-response :as r.resp]))

(defn- to-json-stream [x]
  (reify r.proto/StreamableResponseBody
    (write-body-to-stream [_ _ output-stream]
      (json/write-value output-stream x))))

(defn root-handler [{ds `hikari/datasource} -req]
  (let [pets (jdbc/execute! ds ["select * from pets"])]
    (-> pets
        #_(conj "You don't have to restart the system when you change the code.
        Just uncomment this and eval defn form.")
        to-json-stream
        (r.resp/ok)
        (r.resp/content-type "application/json"))))

(def route-data
  (di/template [["/" {:get {:handler (di/ref `root-handler)}}]]))
