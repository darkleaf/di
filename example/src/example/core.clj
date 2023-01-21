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

(def port (-> (di/ref "PORT")
              (di/fmap parse-long)))

(defn base-registry [{:keys [some-feature-flag]}]
  [{::root           (di/ref `jetty/server)
    ::jetty/handler  (di/ref `reitit/handler)
    ::jetty/options  (di/template {:port (di/ref `port)})
    ::hikari/options (di/template {:adapter "h2"
                                   :url     (di/ref "H2_URL")})}
   (di/update-key `reitit/route-data conj `route-data)
   (di/add-side-dependency `flyway/migrate)
   #_(if some-feature-flag
       [(di/update-key `reitit/route-data conj ...)])])

(defn dev-registry []
  {"PORT"   "8888"
   "H2_URL" "jdbc:h2:mem:test"})

(defonce root (atom nil))

(defn start []
  (reset! root (di/start ::root
                         (base-registry {:some-feature-flag true})
                         (dev-registry))))

(defn stop []
  (di/stop @root))

;; call them from the repl
(comment
  ;; open http://localhost:8888
  (start)
  (stop)
  nil)
