(ns darkleaf.di.opt-ref-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest opt-ref-test
  (with-open [obj (di/start ::root
                            {::root        (di/opt-ref ::replacement)
                             ::replacement ::stub})]
    (t/is (= ::stub @obj))))

(t/deftest opt-ref-missed-test
  (with-open [obj (di/start ::root
                            {::root (-> (di/opt-ref `dep)
                                        (di/fmap (fnil identity :default)))})]
    (t/is (= :default @obj))))

(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/opt-ref :darkleaf.di.opt-ref-test/object"
           (pr-str (di/opt-ref ::object)))))
