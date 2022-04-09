(ns example.adapters.reitit
  (:require
   [reitit.ring :as r]
   [ring.middleware.keyword-params :as r.keyword-params]
   [ring.middleware.params :as r.params]
   [ring.util.http-response :as r.resp]))

(defn default-handler [req]
  (r.resp/not-found))

(defn handler [{route-data ::route-data}]
  (-> route-data
      (r/router)
      (r/ring-handler #'default-handler)))
