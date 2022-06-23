(ns darkleaf.di.opt-ref-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest opt-ref-test
  (with-open [obj (di/start `object
                            {`object     (di/opt-ref `replacement)
                             `replacement ::stub})]
    (t/is (= ::stub @obj))))

(t/deftest opt-ref-n-test
  (with-open [obj (di/start `object
                            {`object (di/opt-ref ::cfg get-in [:a :b :c])
                             ::cfg   {:a {:b {:c ::value}}}})]
    (t/is (= ::value @obj))))


(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/opt-ref darkleaf.di.opt-ref-test/object"
           (pr-str (di/opt-ref `object))))
  (t/is (= "#darkleaf.di.core/opt-ref [darkleaf.di.opt-ref-test/object :key]"
           (pr-str (di/opt-ref `object :key)))))
