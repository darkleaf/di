(ns io.github.darkleaf.di.cases.hooks-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

(t/deftest join-hooks-test
  (let [obj  []
        a    (fn [ident obj] (conj obj ::a))
        b    (fn [ident obj] (conj obj ::b))
        hook (di/join-hooks a b)]
    (t/is (= [::a ::b] (hook ::obj obj)))))
