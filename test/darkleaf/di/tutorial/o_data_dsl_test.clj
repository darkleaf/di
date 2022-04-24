(ns darkleaf.di.tutorial.o-data-dsl-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; It is often to use data-DSLs in Clojure, such as reitit routing,
;; and DI offers tools to handle them easily.
;; Here they are: `di/template` and `di/ref`.

;; I use `def` form only for testing.
;; You should use `defn`:
;; (defn root-handler [-deps req] ...)

(def root-handler (fn [req]))

(def news-handler (fn [req]))

(def route-data
  (di/template
   [["/"     {:get {:handler (di/ref `root-handler)}}]
    ["/news" {:get {:handler (di/ref `news-handler)}}]]))

(t/deftest template-test
  (with-open [system-root (di/start `route-data)]
    (t/is (= [["/"     {:get {:handler root-handler}}]
              ["/news" {:get {:handler news-handler}}]]
             @system-root))))
