(ns darkleaf.di.opt-ref-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn get-ref [{r   ::ref
                :or {r :default}}]
  r)

(t/deftest opt-ref-test
  (with-open [get-ref (di/start `get-ref
                                {::ref         (di/opt-ref ::replacement)
                                 ::replacement ::stub})]
    (t/is (= ::stub (get-ref)))))

(t/deftest opt-ref-missed-test
  (with-open [get-ref (di/start `get-ref
                                {::ref         (di/opt-ref ::replacement)
                                 #_#_
                                 ::replacement ::stub})]
    (t/is (= :default (get-ref)))))

(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/opt-ref :darkleaf.di.opt-ref-test/object"
           (pr-str (di/opt-ref ::object)))))
