(ns example.core
  (:require
   [darkleaf.di.core :as di]
   [next.jdbc :as jdbc]
   [ring.util.http-response :as r.resp]
   [ring.core.protocols :as r.proto]
   [example.adapters.hikari :as-alias hikari]
   [example.adapters.flyway :as-alias flyway]
   [example.adapters.jetty :as-alias jetty]
   [example.adapters.reitit :as-alias reitit]
   [jsonista.core :as json]))

(defn to-json-stream [x]
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
  (di/template [["/" {:get {:handler #'root-handler}}]])) ; or (di/ref `root-handler)

(def base-registry
  {::root              (di/template [(di/ref `jetty/server)
                                     (di/ref `flyway/migrate)])
   ::reitit/route-data #'route-data ; or (di/ref `route-data)
   ::jetty/options     (di/template {:port (di/ref "PORT" parse-long)})
   ::hikari/options    (di/template {:adapter "h2"
                                     :url     (di/ref "H2_URL")})})

(def dev-registry
  {"PORT"   "8888"
   "H2_URL" "jdbc:h2:mem:test"})

(defonce root (atom nil))

(defn start []
  (reset! root (di/start ::root [base-registry
                                 dev-registry
                                 di/ns-registry
                                 di/env-registry])))

(defn stop []
  (di/stop @root))

;; call them from the repl
(comment
  (start)
  (stop)
  nil)
