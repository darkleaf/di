(ns darkleaf.di.tutorial.h-service-host-update-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; We don't need to restart the whole system if we redefine a service.
;; It's very helpful for interactive development.

;; In this test I use a dynamic var but
;; during development you would redefine a static one.

(defn ^:dynamic *service* [-deps x]
  [::result x])

(t/deftest dynamic-service-test
  (let [system-root (di/start `*service*)]
    (do
      (t/is (= [::result 1]
               (system-root 1))))
    (binding [*service* (fn [-deps x] [::other-result x])]
      (t/is (= [::other-result 1]
               (system-root 1))))))
