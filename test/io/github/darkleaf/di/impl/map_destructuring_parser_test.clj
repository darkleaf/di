(ns io.github.darkleaf.di.impl.map-desctructuring-parser-test
  (:require
   [io.github.darkleaf.di.impl.map-destructuring-parser :as sut]
   [clojure.test :as t]))

(t/deftest parse-test
  (t/are [expected input]
      (t/is (= (quote expected)
               (sut/parse (quote input))))
    #{}
    {}

    #{}
    {:as x}

    #{}
    {:or {a 1, b 2}}


    #{:a :b}
    {:keys [a b]}

    #{:a :b}
    {:keys [a b] :or {a :av, b :bv}}

    #{::a ::b}
    {::keys [a b]}

    #{::a ::b}
    {::keys [a b] :or {a :av, b :bv}}


    #{a b}
    {:syms [a b]}

    #{a b}
    {:syms [a b] :or {a :av, b :bv}}

    #{foo/a foo/b}
    {:foo/syms [a b]}

    #{foo/a foo/b}
    {:foo/syms [a b] :or {a :av, b :bv}}


    #{"a" "b"}
    {:strs [a b]}

    #{"a" "b"}
    {:strs [a b] :or {a :av, b :bv}}


    #{:a ::b}
    {a :a, b ::b}

    #{:a ::b}
    {a :a, b ::b, :or {a :av, b :bv}}


    #{a foo/b}
    {a 'a, b 'foo/b}

    #{a foo/b}
    {a 'a, b 'foo/b, :or {a :av, b :bv}}

    #{"a" "b"}
    {a "a", b "b"}))
