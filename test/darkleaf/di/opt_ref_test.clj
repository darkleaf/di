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
  (let [registry {`object (di/opt-ref ::cfg get :port 8080)}]
    (with-open [obj (di/start `object registry {::cfg {:port 9999}})]
      (t/is (= 9999 @obj)))
    (with-open [obj (di/start `object registry)]
      (t/is (= 8080 @obj)))))

(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/opt-ref darkleaf.di.opt-ref-test/object"
           (pr-str (di/opt-ref `object))))
  (t/is (= "#darkleaf.di.core/opt-ref [darkleaf.di.opt-ref-test/object :key]"
           (pr-str (di/opt-ref `object :key)))))
