(ns darkleaf.di.cases.service-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; We don't need to restart the whole system if we redefine a service.
;; It's very helpful for interactive development.
;; In the test I use a dynamic var but
;; during development you could redefine a static one.

(defn ^:dynamic *service* [{} x]
  [::result x])

(t/deftest dynamic-service-test
  (with-open [s (di/start `*service*)]
    (do
      (t/is (= [::result 1] (s 1))))
    (binding [*service* (fn [{} x] [::other-result x])]
      (t/is (= [::other-result 1] (s 1))))))
