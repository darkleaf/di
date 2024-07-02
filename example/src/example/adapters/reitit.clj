(ns example.adapters.reitit
  (:require
   [darkleaf.di.core :as-alias di]
   [reitit.ring :as r]
   [ring.middleware.keyword-params :as r.keyword-params]
   [ring.middleware.params :as r.params]
   [ring.util.http-response :as r.resp]))

(defn default-handler [req]
  (r.resp/not-found))

(def route-data [])

(defn handler
  {::di/kind :component}
  [{route-data `route-data}]
  (-> route-data
      (r/router)
      (r/ring-handler #'default-handler)))
