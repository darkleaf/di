(ns example.core
  (:require
   [darkleaf.di.core :as di]
   [next.jdbc :as jdbc]
   [ring.util.http-response :as r.resp]
   [ring.core.protocols :as r.proto]
   [jsonista.core :as json]))

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
