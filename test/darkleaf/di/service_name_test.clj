(ns darkleaf.di.service-name-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn service [arg])

(t/deftest ok
  (with-open [s (di/start `service)]
    (conj @s 1))
  ,,)

;; It was:
;; class clojure.core$partial$fn__5908 cannot be cast to class
;; clojure.lang.IPersistentCollection (clojure.core$partial$fn__5908
;; and clojure.lang.IPersistentCollection are in unnamed module of
;; loader 'app'

(t/deftest ok2
  (with-open [s (di/start `service)]
    @s)
  ;; => #di/service[#'darkleaf.di.service-name-test/service]
  ,,)
