(ns darkleaf.di.ref-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang ExceptionInfo)))

(t/deftest ref-test
  (with-open [obj (di/start ::root
                            {::root        (di/ref ::replacement)
                             ::replacement ::stub})]
    (t/is (= ::stub @obj))))

(t/deftest ref-missed-test
  (t/is (thrown-with-msg? ExceptionInfo
                          #"\AMissing dependency darkleaf.di.ref-test/dep\z"
                          (di/start ::root
                                    {::root (di/ref `dep)}))))

(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/ref :darkleaf.di.ref-test/object"
           (pr-str (di/ref ::object)))))
