;; # Comparing Integrant and DI for Dependency Injection in Clojure
;;
;; This article compares two approaches to dependency injection in Clojure: [Integrant](https://github.com/weavejester/integrant) and [DI](https://github.com/darkleaf/di).
;; We'll explore their differences through code examples and discuss the pros and cons of each approach.

(ns integrant
  {:nextjournal.clerk/toc true}
  (:require
   [integrant.core :as ig]
   [darkleaf.di.core :as di]))

{:nextjournal.clerk/visibility {:result :hide}}

;; ## Assumptions
;;
;; - All code is written in a single file.
;; - Short namespaces like `:jetty/server` are used instead of `::jetty/server`.
;; - Real dependencies are omitted for brevity. For a complete example, see [the example app](https://github.com/darkleaf/di/tree/master/example).
;;
;; ## Code Examples
;;
;; First, we'll define a fake generic `stop` function to simulate stopping a component:

(defn stop
  "Fake generic stop function"
  [x]
  ;; Placeholder for actual stop logic
  ,)

;; ### Jetty Server
;;
;; #### Using Integrant

(defmethod ig/init-key :jetty/server [_ {:keys [handler port]}]
  ;; In a real application, you might use:
  ;; (jetty/run-jetty handler {:port port, :join? false})
  [:jetty handler port])

(defmethod ig/halt-key! :jetty/server [_ server]
  (stop server))

;; #### Using DI

(defn jetty-server
  {::di/stop #(stop %)}
  [{handler :jetty/handler
    port    "PORT"}]
  [:jetty handler port])

;; With DI:
;;
;; - Use **symbols** for var names.
;; - Use **keywords** for abstract dependencies to define them later.
;; - Use **strings** for environment variables.
;;
;; ### Routes
;;
;; #### Using Integrant

(defmethod ig/init-key :web/route-data [_ {:keys [root-handler]}]
  [["/" {:get {:handler root-handler}}]])

;; #### Using DI

(def route-data
  (di/template
   [["/" {:get {:handler (di/ref `root-handler)}}]]))

;; Here, `(di/ref `root-handler)` resolves to the `root-handler` var.
;; There's no need to define `root-handler` as a component in the system config.
;;
;; ### Reitit
;;
;; We need to transform `route-data` into a Ring handler.

(defn route-data->handler [route-data]
  ;; In a real application, you might use:
  ;; (-> route-data
  ;;     (ring/router)
  ;;     (ring/ring-handler))
  [:ring-handler route-data])

;; #### Using Integrant

(defmethod ig/init-key :web/handler [_ {:keys [route-data]}]
  (route-data->handler route-data))

;; #### Using DI

(def web-handler (di/derive `route-data route-data->handler))

;; Alternatively:
;;
;; ```clojure
;; (defn web-handler [{route-data `route-data}]
;;   (route-data->handler route-data))
;; ```
;;
;; Or:
;;
;; ```clojure
;; (defn web-handler [{:syms [route-data]}]
;;   (route-data->handler route-data))
;; ```
;;
;; ### Handlers
;;
;; #### Using Integrant

(defmethod ig/init-key :web/root-handler [_ {:keys []}]
  (fn [req]
    :ok))

;; #### Using DI

(defn root-handler [-deps req]
  :ok)

;; With DI, you don't need to restart the system while developing `root-handler`.
;; Just re-evaluate the `defn` form.
;; With Integrant, you'd need to use `ig/suspend-key!` and `ig/resume-key`
;; to preserve the system state.
;;
;; ### System Initialization
;;
;; #### Using Integrant

(def ig-config
  {:jetty/server     {:port    8080
                      :handler (ig/ref :web/handler)}
   :web/route-data   {:root-handler (ig/ref :web/root-handler)}
   :web/handler      {:route-data (ig/ref :web/route-data)}
   :web/root-handler {}})

(ig/init ig-config)

;; #### Using DI
;;

(di/start `jetty-server {"PORT"         8080
                         :jetty/handler (di/ref `web-handler)})

;; In DI:
;;
;; - `:jetty/handler` is an abstraction.
;; - You don't need to depend on a concrete key in the namespace with
;;   the Jetty component.
;; - Only declare abstract dependencies in the registry.
;; - The registry allows you to override any key.
;;
;; ## Real Applications
;;
;; In a real application, you might have multiple databases and layers.
;;
;; > The vassal of my vassal is not my vassal.
;;
;; Components should declare their dependencies themselves.
;; For example, a handler depends on a database connection and an auth token decoder,
;; and the decoder depends on a secret key from an environment variable.
;;
;; ### Handling Dependencies
;;
;; With Integrant or Component, you have options for linking handlers and stateful components:
;;
;; 1. **Make every stateless handler a component.**
;; - Results in a lot of configuration.
;; 2. **Pass stateful components via the Ring request.**
;; - [Example](https://github.com/prestancedesign/usermanager-reitit-example/blob/main/src/usermanager/controllers/user.clj#L73).
;;
;; With DI, you can define dependencies directly:

(defn root-handler* [{db      :db/datasource
                      decoder `token-decoder}
                     req]
  :ok)

(defn token-decoder [{key "SECRET_KEY"} token]
  :decoded)

;; This allows you to define as many stateless components as needed,
;; which is the main goal of DI.
;;
;; ## Subsystems
;;
;; In a web application with independent subsystems
;; (e.g., `/subsystem-a`, `/subsystem-b`),
;; each subsystem has its own `route-data` and uses common components.
;;
;; ### Using DI to Extend Routes
;;
;; With `di/update-key`,
;; you can extend base route-data with specific one in each subsystem.

(def subsystem-a-route-data
  [["/subsystem-a/" '...]])

(defn subsystem-a []
  (di/update-key `route-data concat (di/ref `subsystem-a-route-data)))

;; Starting the system:

(di/start `jetty-server
          {"PORT"         8080
           :jetty/handler (di/ref `web-handler)}
          (subsystem-a)
          #_(subsystem-N))

;; ## Feature Flags
;;
;; In most cases I prefer to use feature flags instead of branching.
;; It can be implemented easily with DI:

(defn subsystem-a* [{:keys [subsystem-a-enabled]}]
  (when subsystem-a-enabled
    (di/update-key `route-data concat (di/ref `subsystem-a-route-data))))

(defn registry [flags]
  [{"PORT"         8080
    :jetty/handler (di/ref `web-handler)}
   (subsystem-a* flags)
   #_(subsystem-N flags)])

(di/start `jetty-server (registry {:subsystem-a-enabled true}))

;; ## Error Handling
;;
;; Both Integrant and Component can hinder REPL development
;; if `ig/init-key` throws an exception.
;; For example, if the Jetty server starts and listens on a port
;; but initialization fails, you might need to restart the REPL.
;;
;; DI is smart enough to stop already started components in such cases.
;;
;; *Note*: You can use [integrant-repl](https://github.com/weavejester/integrant-repl), which handles this scenario by [stopping components on failure](https://github.com/weavejester/integrant-repl/blob/master/src/integrant/repl.clj#L22).
;;
;; ## Aspect-Oriented Programming (AOP)
;;
;; While AOP can be a dangerous practice,
;; sometimes you need to add extra behavior to existing components
;; (e.g., logging, performance measurement, spec checking).
;;
;; With Integrant, there's no convenient way to update an existing component.
;; You'd need to [rename the component key](https://github.com/weavejester/integrant/issues/58) and reconfigure dependent components.
;;
;; With DI, you can use `di/update-key` and `di/instrument` to modify components.
;;
;; ---
;;
;; This comparison highlights how DI provides a flexible and dynamic approach
;; to dependency injection in Clojure,
;; especially in terms of REPL-driven development and managing complex dependencies.
;; Integrant, while powerful, may require more boilerplate during development.
