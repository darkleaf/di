(ns darkleaf.di.impl.map-desctructuring-parser-test
  (:require
   [darkleaf.di.impl.map-destructuring-parser :as sut]
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


    {:a nil, :b nil}
    {:keys [a b]}

    {:a :av, :b :bv}
    {:keys [a b] :or {a :av, b :bv}}

    {::a nil, ::b nil}
    {::keys [a b]}

    {::a :av, ::b :bv}
    {::keys [a b] :or {a :av, b :bv}}


    {a nil, b nil}
    {:syms [a b]}

    {a :av, b :bv}
    {:syms [a b] :or {a :av, b :bv}}

    {foo/a nil, foo/b nil}
    {:foo/syms [a b]}

    {foo/a :av, foo/b :bv}
    {:foo/syms [a b] :or {a :av, b :bv}}


    {"a" nil, "b" nil}
    {:strs [a b]}

    {"a" :av, "b" :bv}
    {:strs [a b] :or {a :av, b :bv}}


    {:a nil, ::b nil}
    {a :a, b ::b}

    {:a :av, ::b :bv}
    {a :a, b ::b, :or {a :av, b :bv}}


    {a nil, foo/b nil}
    {a 'a, b 'foo/b}

    {a :av, foo/b :bv}
    {a 'a, b 'foo/b, :or {a :av, b :bv}}

    {"a" nil, "b" nil}
    {a "a", b "b"}))
