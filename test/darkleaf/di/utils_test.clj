(ns darkleaf.di.utils-test
  (:require
   [darkleaf.di.utils :as sut]
   [darkleaf.di.protocols :as p]
   [clojure.test :as t]))

(t/deftest delegate-factory-test
  (let [obj     (reify
                  p/Factory
                  (dependencies [_]
                    :deps)
                  (build [_ deps]
                    [:build deps])
                  (demolish [_ obj]
                    [:demolish obj]))
        factory (sut/delegate-factory obj
                  clojure.lang.IDeref
                  (deref [_] obj))]
    (t/is (= (p/dependencies obj)
             (p/dependencies factory)))
    (t/is (= (p/build obj :deps)
             (p/build factory :deps)))
    (t/is (= (p/demolish obj :obj)
             (p/demolish factory :obj)))
    (t/is (= obj @factory))))


#_
(t/deftest delegate-test
  (let [obj     (reify
                  p/Factory
                  (dependencies [_]
                    :deps)
                  (build [_ deps]
                    [:build deps])
                  (demolish [_ obj]
                    [:demolish obj])
                  p/FactoryDescription
                  (description [_]
                    :description))
        factory (sut/delegate
                    {p/Factory            obj
                     p/FactoryDescription obj}
                  clojure.lang.IDeref
                  (deref [_] obj))]
    (t/is (= (p/dependencies obj)
             (p/dependencies factory)))
    (t/is (= (p/build obj :deps)
             (p/build factory :deps)))
    (t/is (= (p/demolish obj :obj)
             (p/demolish factory :obj)))
    (t/is (= (p/description obj)
             (p/description factory)))
    (t/is (= obj @factory))))
