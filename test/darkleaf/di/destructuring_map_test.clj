(ns darkleaf.di.destructuring-map-test
  (:require
   [darkleaf.di.destructuring-map :as sut]
   [clojure.test :as t]))

(t/deftest deps-test
  (t/are [expected input]
      (t/is (= (quote expected)
               (sut/dependencies (quote input))))
    {}
    {}

    {}
    {:as x}

    {}
    {:or {a 1, b 2}}


    {:a :required, :b :required}
    {:keys [a b]}

    {:a :optional, :b :optional}
    {:keys [a b] :or {a :av, b :bv}}

    {::a :required, ::b :required}
    {::keys [a b]}

    {::a :optional, ::b :optional}
    {::keys [a b] :or {a :av, b :bv}}


    {a :required, b :required}
    {:syms [a b]}

    {a :optional, b :optional}
    {:syms [a b] :or {a :av, b :bv}}

    {foo/a :required, foo/b :required}
    {:foo/syms [a b]}

    {foo/a :optional, foo/b :optional}
    {:foo/syms [a b] :or {a :av, b :bv}}


    {"a" :required, "b" :required}
    {:strs [a b]}

    {"a" :optional, "b" :optional}
    {:strs [a b] :or {a :av, b :bv}}


    {:a :required, ::b :required}
    {a :a, b ::b}

    {:a :optional, ::b :optional}
    {a :a, b ::b, :or {a :av, b :bv}}


    {a :required, foo/b :required}
    {a 'a, b 'foo/b}

    {a :optional, foo/b :optional}
    {a 'a, b 'foo/b, :or {a :av, b :bv}}

    {"a" :required, "b" :required}
    {a "a", b "b"}

    {"a" :optional, "b" :optional}
    {a "a", b "b", :or {a :av, b :bv}}))
