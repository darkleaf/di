(ns io.github.darkleaf.di.impl.map-destructuring-parser-test
  (:require
   [io.github.darkleaf.di.impl.map-destructuring-parser :as sut]
   [clojure.test :as t]))

(t/deftest parse-test
  (t/are [expected input]
      (t/is (= (quote expected)
               (sut/parse (quote input))))
    {}
    {}

    {}
    {:as x}

    {}
    {:or {a 1, b 2}}


    {:a true, :b true}
    {:keys [a b]}

    {:a false, :b false}
    {:keys [a b] :or {a :av, b :bv}}

    {::a true, ::b true}
    {::keys [a b]}

    {::a false, ::b false}
    {::keys [a b] :or {a :av, b :bv}}


    {a true, b true}
    {:syms [a b]}

    {a false, b false}
    {:syms [a b] :or {a :av, b :bv}}

    {foo/a true, foo/b true}
    {:foo/syms [a b]}

    {foo/a false, foo/b false}
    {:foo/syms [a b] :or {a :av, b :bv}}


    {"a" true, "b" true}
    {:strs [a b]}

    {"a" false, "b" false}
    {:strs [a b] :or {a :av, b :bv}}


    {:a true, ::b true}
    {a :a, b ::b}

    {:a false, ::b false}
    {a :a, b ::b, :or {a :av, b :bv}}


    {a true, foo/b true}
    {a 'a, b 'foo/b}

    {a false, foo/b false}
    {a 'a, b 'foo/b, :or {a :av, b :bv}}

    {"a" true, "b" true}
    {a "a", b "b"}

    {"a" false, "b" false}
    {a "a", b "b", :or {a :av, b :bv}}))
