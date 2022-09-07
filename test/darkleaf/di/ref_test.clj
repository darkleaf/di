(ns darkleaf.di.ref-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest ref-test
  (with-open [obj (di/start `object
                            {`object     (di/ref `replacement)
                             `replacement ::stub})]
    (t/is (= ::stub @obj))))

(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/ref darkleaf.di.ref-test/object"
           (pr-str (di/ref `object)))))
