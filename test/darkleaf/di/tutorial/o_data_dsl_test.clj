;; # Data DSL

^{::clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.o-data-dsl-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; It is often to use data-DSLs in Clojure, such as reitit routing,
;; and DI offers tools to handle them easily.
;; Here they are: `di/template` and `di/ref`.

;; You can also use `di/opt-ref` for optional dependencies.
;; If there is no defined key opt-ref resolves to nil.

(def route-data
  (di/template
   [["/"     {:get {:handler (di/ref `root-handler)}}]
    ["/news" {:get {:handler (di/ref `news-handler)}}]]))

(t/deftest template-test
  (letfn [(root-handler [req])
          (news-handler [req])]
    (with-open [root (di/start `route-data {`root-handler root-handler
                                            `news-handler news-handler})]
      (t/is (= [["/"     {:get {:handler root-handler}}]
                ["/news" {:get {:handler news-handler}}]]
               @root)))))
