(ns example.adapters.hikari
  (:require
   [hikari-cp.core :as hikari]
   [darkleaf.di.core :as-alias di]))

(defn datasource
  {::di/stop hikari/close-datasource}
  [{options ::options}]
  (hikari/make-datasource options))
