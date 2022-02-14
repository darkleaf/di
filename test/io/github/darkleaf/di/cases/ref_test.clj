(ns io.github.darkleaf.di.cases.ref-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

(t/deftest ref-form-test
  (with-open [obj (di/start ::object
                            {::object (di/ref-form {:a (di/ref ::a)
                                                    :b [(di/ref ::b)]
                                                    :c #{1 (di/ref ::c)}})
                             ::a 1
                             ::b 2
                             ::c 3})]
    (t/is (= {:a 1
              :b [2]
              :c #{1 3}}
             @obj))))
