^{::clerk/visibility {:code :hide}}
(ns index
  {:nextjournal.clerk/toc true}
  (:require
   [nextjournal.clerk :as clerk]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as dip]))

{::clerk/visibility {:code :hide}}

;; <!-- [![Clojars Project](https://img.shields.io/clojars/v/org.clojars.darkleaf/di.svg)](https://clojars.org/org.clojars.darkleaf/di) -->

;; # Dependency injection

;; DI is a dependency injection framework that allows you to define dependencies as cheaply as defining function arguments.

;; It uses plain clojure functions and [associative destructuring](https://clojure.org/guides/destructuring#_associative_destructuring)
;; to define a graph of functions and stateful objects.

;; ```clojure
;; (defn handler [{get-user `get-user} ring-req]
;;   ...
;;   (get-user user-id)
;;   ...)
;;
;; (defn get-user [{ds ::db/datasource} id]
;;   ...)
;;
;; (defn jetty
;;   {::di/stop #(.stop %)}
;;   [{handler `handler
;;     port    "PORT"}]
;;   (jetty/run-jetty handler {:join? false, :port port}))
;;
;; (di/start `jetty)
;; ```

;; ## Install

;; ```edn
;; {:deps {org.clojars.darkleaf/di {:git/url "https://github.com/darkleaf/di.git"
;;                                  :sha     "ce41a37e7217c30886535fc0588bc6e452ce93c6"}}}
;; ```

;; ## Tutorial

;; Each chapter is a regular Clojure test namespace.
;; You can clone the repo and run each one in the REPL.

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
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/p_fmap_test.clj")} "Fmap"]]])

;; ### Advanced
(clerk/html
 [:ul
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/x_add_side_dependency_test.clj")} "Add a side dependency"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/x_instrument_test.clj")} "Instrument"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/x_update_key_test.clj")} "Update key"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/y_graceful_stop_test.clj")} "Graceful stop"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/y_multi_arity_service_test.clj")} "Multi arity service"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/z_multi_system_test.clj")} "Multi system"]]
  [:li [:a {:href (clerk/doc-url "test/darkleaf/di/tutorial/z_two_databases_test.clj")} "Two Databases"]]])

;; ## Example app

;; [Example app](https://github.com/darkleaf/di/tree/master/example)

;; Start with [user.clj](https://github.com/darkleaf/di/blob/master/example/dev/user.clj)

;; ## API

^{::clerk/visibility {:result :hide}}
(defn view-doc [var]
  (clerk/html
   [:<>
    [:h4
     [:code (-> var symbol str)]]
    (-> var meta :arglists clerk/code)
    (-> var meta :doc clerk/md)]))

;; ### `darkleaf.di.core`
(view-doc #'di/start)
(view-doc #'di/stop)
(view-doc #'di/ref)
(view-doc #'di/opt-ref)
(view-doc #'di/template)
(view-doc #'di/fmap)
(view-doc #'di/instrument)
(view-doc #'di/update-key)
(view-doc #'di/add-side-dependency)
(view-doc #'di/combine-dependencies)
;; ### `darkleaf.di.protocols`
(view-doc #'dip/stop)
(view-doc #'dip/dependencies)
(view-doc #'dip/build)

;; ## License
;; Copyright © 2022 Mikhail Kuzmin
;;
;; Licensed under Eclipse Public License v2.0.
