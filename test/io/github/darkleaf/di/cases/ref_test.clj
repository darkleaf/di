(ns io.github.darkleaf.di.cases.ref-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

(t/deftest ref-test
  (with-open [obj (di/start `object
                            {`object     (di/ref `replacement)
                             `replacement ::stub})]
    (t/is (= ::stub @obj))))


(t/deftest ref-n-test
  (with-open [obj (di/start `object
                            {`object (di/ref ::cfg get-in [:a :b :c])
                             ::cfg   {:a {:b {:c ::value}}}})]
    (t/is (= ::value @obj))))

