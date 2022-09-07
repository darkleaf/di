(ns darkleaf.di.opt-ref-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest opt-ref-test
  (let [registry {::root (di/template {:ref (di/opt-ref ::ref)})}]
    (with-open [obj (di/start ::root
                               registry
                               {::ref  :value})]
      (t/is (= {:ref :value} @obj)))
    (with-open [obj (di/start ::root
                              registry)]
      (t/is (= {:ref nil} @obj)))))


(t/deftest opt-ref-not-found-test
  (let [registry {::root (di/template {:ref (di/opt-ref ::ref :default)})}]
    (with-open [obj (di/start ::root
                              registry
                              {::ref :value})]
      (t/is (= {:ref :value} @obj)))
    (with-open [obj (di/start ::root
                              registry)]
      (t/is (= {:ref :default} @obj)))))

(t/deftest opt-ref-not-found-factory-test
  (let [registry {::root (di/template {:ref (di/opt-ref ::ref (di/ref ::default))})}]
    #_(with-open [obj (di/start ::root
                                registry
                                {::ref :value})]
        (t/is (= {:ref :value} @obj)))
    #_(with-open [obj (di/start ::root
                                registry
                                {::default :default})]
        (t/is (= {:ref :default} @obj)))))


(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/opt-ref darkleaf.di.opt-ref-test/object"
           (pr-str (di/opt-ref `object))))
  (t/is (= "#darkleaf.di.core/opt-ref [darkleaf.di.opt-ref-test/object :default]"
           (pr-str (di/opt-ref `object :default)))))
