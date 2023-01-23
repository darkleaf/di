;; # Comparison of Integrant and DI

(ns integrant
  {:nextjournal.clerk/toc true}
  (:require
   [integrant.core :as ig]
   [darkleaf.di.core :as di]))

{:nextjournal.clerk/visibility {:result :hide}}

;; ## Assumptions
;; Here I'm writing code in a single file and
;; I have to use short namespaces like `:jetty/server` instead of `::jetty/server`.
;;
;; Here I'm not using real dependencies to keep it short.
;; Please see [the example app](https://github.com/darkleaf/di/tree/master/example).

;; ## Code
(defn stop
  "Fake generic stop function"
  [x])

;; ### Jetty

(defmethod ig/init-key :jetty/server [_ {:keys [handler port]}]
  ;; (jetty/run-jetty handler {:port port, :join? false})
  [:jetty handler port])

(defmethod ig/halt-key! :jetty/server [_ server]
  (stop server))

(defn jetty-server
  {::di/stop #(stop %)}
  [{handler :jetty/handler
    port    "PORT"}]
  [:jetty handler port])

;; With DI, use symbols for var names,
;; keywords for abstract dependencies to define them later,
;; and strings for environment variables.

;; ### Routes

(defmethod ig/init-key :web/route-data [_ {:keys [root-handler]}]
  [["/" {:get {:handler root-handler}}]])

(def route-data
  (di/template
   [["/" {:get {:handler (di/ref `root-handler)}}]]))

;; ``(di/ref `root-handler)`` resolves to `root-handler` var.
;; There is no need to define root-handler as a component in the system config.

;; ### Reitit

(defn route-data->handler [route-data]
  #_(-> route-data
        (ring/router)
        (ring/ring-handler))
  :ring-handler)

(defmethod ig/init-key :web/handler [_ {:keys [route-data]}]
   (route-data->handler route-data))

(def web-handler
  (-> (di/ref `route-data)
      (di/fmap route-data->handler)))

;; You can also use these variants as well:
;; ```clojure
;; (defn web-handler [{route-data `route-data}]
;;   (route-data->handler route-data))
;;
;; (defn web-handler [{:syms [route-data]}]
;;   (route-data->handler route-data))
;; ```

;; ### Handlers

(defmethod ig/init-key :web/root-handler [_ {:keys []}]
  (fn [req]
    :ok))

(defn root-handler [-deps req]
  :ok)

;; Instead of Integrant, with DI you don't have to restart the system while
;; developing `root-handler`. Just eval defn form again.

;; With Integrant, you have to use complicated `ig/suspend-key!` and `ig/resume-key`
;; to preserve the state of the system.

;; ### System

(def ig-config
  {:jetty/server     {:port    8080
                      :handler (ig/ref :web/handler)}
   :web/route-data   {:root-handler (ig/ref :web/root-handler)}
   :web/handler      {:route-data (ig/ref :web/route-data)}
   :web/root-handler {}})

(ig/init ig-config)

(di/start `jetty-server {"PORT"         8080
                         :jetty/handler (di/ref `web-handler)})

;; `:jetty/handler` is an abstraction.
;; I don't need to depend on a concrete key in the namespace with the jetty component.

;; With DI, you only need to declare abstract dependencies in the registry.
;; Also, with the registry, you can override any key.

;; ## Real applications

;; That was a trivial example.
;; In my application I'm using two different databases and may add a third.

;; > Vassal of my vassal is not my vassal
;;
;; I have layers in my application.
;; I think, my components should declare dependencies by themselves.
;; My handler depends on database connection and auth token decoder.
;; And the decoder depends on secret key as an environment variable.

;; With Integrant or Component you have options for how to link handlers and
;; stateful components:
;; 1. Make every stateless handler a component.
;; And you have a lot of configuration.
;; 2. [Pass](https://github.com/prestancedesign/usermanager-reitit-example/blob/main/src/usermanager/controllers/user.clj#L73)
;; all stateful components (except Jetty) with a ring request.

;; With DI you have a better solution.
;; Don't touch anything except the handler, just define a new dependency:

(comment
  (defn root-handler [-deps req]
    :ok))

(defn root-handler* [{db      :db/datasource
                      decoder `token-decoder}
                     req]
  :ok)

(defn token-decoder [{key "SECRET_KEY"} token]
  :decoded)

;; Allowing you to easily define as many stateless components as you need is the main goal of DI.

;; ## Subsystems

;; In my web application I have independent subsustems like
;; * /subsystem-a
;; * /subsystem-b
;; * /etc


;; Each subsystem has its own route-data and uses only common components.

;; With `di/update-key` I extend base route-data with specific one in each subsystem.

(def subsystem-a-route-data [["/subsystem-a/" '...]])

(defn subsystem-a []
  (di/update-key `route-data concat `subsystem-a-route-data))

(di/start `jetty-server
          {"PORT"         8080
           :jetty/handler (di/ref `web-handler)}
          (subsystem-a)
          #_(subsystem-N))

;; ## Error handling

;; Both Integrant and Component break REPL development.
;; What happens if `ig/init-key` throws an exception?
;; You have started the Jetty server and it is listening on port.
;; You have to restart the REPL to continue development.

;; DI is smart enough to stop already started components.

;; ## AOP

;; I think AOP is quite dangerous practice.
;; But some times I need to add extra behavior to my existing component without touching it.
;; Like logging, performance measurement, spec checking.
;; With Integrant, there is no convenient way to update an existing component.
;; You have to [rename](https://github.com/weavejester/integrant/issues/58)
;; the key of you component, and reconfigure all dependent components.

;; With DI, you could use `di/update-key` and `di/instrument`.
