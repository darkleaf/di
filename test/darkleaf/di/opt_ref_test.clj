(ns darkleaf.di.opt-ref-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest opt-ref-test
  (with-open [obj (di/start ::root
                            {::root       (di/opt-ref `replacement)
                             `replacement ::stub})]
    (t/is (= ::stub @obj))))

;; todo: use bind
#_(t/deftest opt-ref-missed-test
    (with-open [obj (di/start ::root
                              {::root (di/opt-ref `dep)})]
      (t/is (nil? @obj))))

#_(t/deftest pr-test
    (t/is (= "#darkleaf.di.core/opt-ref darkleaf.di.opt-ref-test/object"
             (pr-str (di/opt-ref `object))))
    (t/is (= "#darkleaf.di.core/opt-ref [darkleaf.di.opt-ref-test/object :key]"
             (pr-str (di/opt-ref `object :key)))))
