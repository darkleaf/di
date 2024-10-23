^{::clerk/visibility {:code :hide}}
(ns index
  {:nextjournal.clerk/toc true}
  (:require
   [nextjournal.clerk :as clerk]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as dip]))

{::clerk/visibility {:code :hide}}

;; # Dependency injection

;; [DI](https://github.com/darkleaf/di) is a dependency injection framework
;; that allows you to define dependencies as easily as you define function arguments.

;; It uses plain clojure functions and [associative destructuring](https://clojure.org/guides/destructuring#_associative_destructuring)
;; to define a graph of functions and stateful objects.

;; ```clojure
;; (ns app.core
;;   (:require
;;    [darkleaf.di.core :as di]
;;    [ring.adapter.jetty :as jetty]
;;    [app.adapters.reitit :as-alias reitit]
;;    [app.adapters.hikari :as-alias hikari]
;;    [app.adapters.db     :as-alias db]))
;;
;; (defn show-user [{ds ::db/datasource} req]
;;   ...)
;;
;; (def route-data
;;   (di/template
;;     [["/users/:id" {:get {:handler (di/ref `show-user)}}]]))
;;
;; (defn jetty
;;   {::di/stop (memfn stop)}
;;   [{handler ::handler
;;     port    :env.long/PORT
;;     :or     {port 8080}
;;   (jetty/run-jetty handler {:join? false, :port port}))
;;
;; (di/start `jetty
;;           (di/env-parsing :env/long parse-long)
;;           {::handler           (di/ref `reitit/handler)
;;            ::reitit/route-data (di/ref `reitit/data)
;;            ::db/datasource     (di/ref `hikari/datasource)
;;            "PORT"              "9090"})
;; ```

;; It is just a short snippet, please see [example app](https://github.com/darkleaf/di/tree/master/example).

;; ## Install

;; [![Clojars Project](https://img.shields.io/clojars/v/org.clojars.darkleaf/di.svg)](https://clojars.org/org.clojars.darkleaf/di)

;; ```edn
;; {:deps {org.clojars.darkleaf/di {:mvn/version "2.0.0"}}}
;; ;; or
;; {:deps {org.clojars.darkleaf/di {:git/url "https://github.com/darkleaf/di.git"
;;                                  :sha     "%SHA%"}}}
;; ```

;; ## Comparison with other tools

(clerk/html
  [:a {:href (clerk/doc-url "notebooks/integrant.clj")} "Comparison of Integrant and DI"])

;; ## Tutorial

;; Each chapter is a regular Clojure test namespace.
;; You can clone [the repo](https://github.com/darkleaf/di) and run each one in the REPL.

;; ### Base
(clerk/html
 [:ul
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/a_intro_test.clj")} "Intro"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/b_dependencies_test.clj")} "Dependencies"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/c_stop_test.clj")} "Stop"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/l_registries_test.clj")} "Registries"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/m_abstractions_test.clj")} "Abstractions"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/n_env_test.clj")} "Env"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/o_data_dsl_test.clj")} "Data DSL"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/p_fmap_test.clj")} "Fmap"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/q_starting_many_keys_test.clj")} "Staring many keys"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/r_multimethods_test.clj")} "Multimethods"]]])

;; ### Advanced
(clerk/html
 [:ul
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/x_add_side_dependency_test.clj")} "Add a side dependency"]]
  #_[:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/x_instrument_test.clj")} "Instrument"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/x_update_key_test.clj")} "Update key"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/x_log_test.clj")} "Log"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/y_graceful_stop_test.clj")} "Graceful stop"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/y_multi_arity_service_test.clj")} "Multi arity service"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/z_multi_system_test.clj")} "Multi system"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/z_two_databases_test.clj")} "Two Databases"]]])

;; ## Example app

;; [Example app](https://github.com/darkleaf/di/tree/master/example)

;; Start with [user.clj](https://github.com/darkleaf/di/blob/master/example/dev/user.clj)

;; ## API

;; [![cljdoc badge](https://cljdoc.org/badge/org.clojars.darkleaf/di)](https://cljdoc.org/d/org.clojars.darkleaf/di/CURRENT)

^{::clerk/visibility {:result :hide}}
(defn view-doc [var]
  (clerk/html
   [:<>
    [:h4
     [:code (-> var symbol str)]]
    (some-> var meta :arglists clerk/code)
    (some-> var meta :doc clerk/md)]))

;; ### `darkleaf.di.core`
(view-doc #'di/start)
(view-doc #'di/stop)
(view-doc #'di/ref)
(view-doc #'di/opt-ref)
(view-doc #'di/template)
(view-doc #'di/derive)
(view-doc #'di/env-parsing)
#_(view-doc #'di/instrument)
(view-doc #'di/update-key)
(view-doc #'di/add-side-dependency)
(view-doc #'di/ns-publics)
(view-doc #'di/combine-dependencies)
;; ### `darkleaf.di.protocols`
;; `darkleaf.di.protocols/Factory`
(view-doc #'dip/dependencies)
(view-doc #'dip/build)
(view-doc #'dip/demolish)

;; ## License
;; Copyright © 2022 Mikhail Kuzmin
;;
;; Licensed under Eclipse Public License v2.0.
