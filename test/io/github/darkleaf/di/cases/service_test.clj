(ns io.github.darkleaf.di.cases.service-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

;; `{}` means an service has no deps.
(defn service [{} arg1 arg2]
  [::result arg1 arg2])

(t/deftest start-service-test
  (with-open [s (di/start `service)]
    ;; `s` implements IDeref
    (t/is (= [::result 1 2] (@s 1 2)))
    ;; `s` implements IFn
    (t/is (= [::result 1 2] (s 1 2)))))


;; We don't need to restart the whole system if we redefine a service.
;; It's very helpful for interactive development.
;; In the test I use dynamic var but
;; during development you could redefine the static var.

(defn ^:dynamic *service* [{} arg1]
  [::result arg1])

(t/deftest dynamic-service-test
  (with-open [s (di/start `*service*)]
    (binding [*service* (fn [{} arg1]
                          [::other-result arg1])]
      (t/is (= [::other-result 1] (s 1))))))
