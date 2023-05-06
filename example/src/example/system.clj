(ns example.system
  (:require
   [darkleaf.di.core :as di]
   [example.adapters.hikari :as-alias hikari]
   [example.adapters.flyway :as-alias flyway]
   [example.adapters.jetty :as-alias jetty]
   [example.adapters.reitit :as-alias reitit]
   [example.core :as core]))

(defn base-registry [{:keys [some-feature-flag]}]
  [{::root           (di/ref `jetty/server)
    ::jetty/handler  (di/ref `reitit/handler)
    ::hikari/options (di/template {:adapter "h2"
                                   :url     (di/ref "H2_URL")})}
   (di/update-key `reitit/route-data conj `core/route-data)
   (di/add-side-dependency `flyway/migrate)
   (di/env-parsing :env.long parse-long)
   #_(if some-feature-flag
       [(di/update-key `reitit/route-data conj ...)])])

(defn dev-registry []
  (let [flags  {:some-feature-flag true}]
    [(base-registry flags)
     {"PORT"   "8888"
      "H2_URL" "jdbc:h2:mem:test"}]))
