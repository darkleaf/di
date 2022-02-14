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


(t/deftest ref-map-test
  (with-open [obj (di/start `object
                            {`object (di/ref-map #{`a ::b "c" :d})
                             `a      1
                             ::b     2
                             "c"     3})]
    (t/is (= {`a 1, ::b 2, "c" 3, :d nil} @obj))))


(t/deftest ref-map-n-test
  (with-open [obj (di/start `object
                            {`object (di/ref-map #{`a ::b "c" :d} assoc :e 4)
                             `a     1
                             ::b    2
                             "c"    3})]
    (t/is (= {`a 1, ::b 2, "c" 3, :d nil, :e 4} @obj))))


(t/deftest ref-form-test
  (with-open [obj (di/start `object
                            {`object (di/ref-form {:a (di/ref `a)
                                                   :b [(di/ref `b)]
                                                   :c #{1 (di/ref `c)}})
                             `a 1
                             `b 2
                             `c 3})]
    (t/is (= {:a 1
              :b [2]
              :c #{1 3}}
             @obj))))
