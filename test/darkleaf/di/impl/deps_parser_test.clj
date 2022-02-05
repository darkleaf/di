(ns darkleaf.di.impl.deps-parser-test
  (:require
   [darkleaf.di.impl.deps-parser :as sut]
   [clojure.test :as t]))

(t/deftest parse-destructuring-map-test
  (t/are [expected input]
      (t/is (= (quote expected)
               (sut/parse-destructuring-map (quote input))))
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


    {:a nil, ::b nil}
    {a :a, b ::b}

    {:a :av, ::b :bv}
    {a :a, b ::b, :or {a :av, b :bv}}


    {a nil, foo/b nil}
    {a 'a, b 'foo/b}

    {a :av, foo/b :bv}
    {a 'a, b 'foo/b, :or {a :av, b :bv}}))







(defn no-opts
  [{}])

(t/deftest no-opts-test
  (t/is (= {}
           (parse #'no-opts))))


;; запретить такое ??
;; (defn configs
;;   [{:keys [a] :syms [b] :strs [c]}])

;; (t/deftest configs-test
;;   (t/is (= {:all      #{}
;;             :optional #{}}
;;            (parse #'configs))))


;; (defn configs-with-defaults
;;   [{:keys [a] :syms [b] :strs [c]
;;     :or   {a 1 b 2 c 3}}])

;; (t/deftest configs-with-defaults-test
;;   (t/is (= {:all      #{}
;;             :optional #{}}
;;            (parse #'configs-with-defaults))))

(comment

  (defn services
    [{::syms [a] ::t/syms [b]}])

  (t/deftest services-test
    (t/is (= {`a nil, `t/b nil}
             (parse #'services))))

  (defn states
    [{::keys [a] ::t/keys [b]}])

  (t/deftest states-test
    (t/is (= {:all      #{::a ::t/b}
              :optional #{}}
             (parse #'states))))


  (defn services-with-defaults
    [{::syms [a] :or {a :default}}])

  (t/deftest services-with-defaults-test
    (t/is (= {:all      #{`a}
              :optional #{`a}}
             (parse #'services-with-defaults))))


  (defn states-with-defaults
    [{::keys [a] ::t/keys [b] :or {a :default}}])

  (t/deftest states-with-defaults-test
    (t/is (= {:all      #{::a ::t/b}
              :optional #{::a}}
             (parse #'states-with-defaults))))


  (defn named
    [{a :a/b, b 'c/d}])

  (t/deftest named-test
    (t/is (= {:all      #{:a/b 'c/d}
              :optional #{}}
             (parse #'named))))


  (defn custom-with-defaults
    [{a   :a/b, b 'c/d
      :or {a :default}}])

  (t/deftest custom-with-defaults-test
    (t/is (= {:all      #{:a/b 'c/d}
              :optional #{:a/b}}
             (parse #'custom-with-defaults))))


  (defn multi-arity
    ([{::syms [a]}])
    ([{::syms [a b]} a1]))

  (t/deftest multi-arity-test
    (t/is (= {:all      #{`a `b}
              :optional #{}}
             (parse #'multi-arity))))


  (defn multi-arity-with-defaults
    ([{::syms [a] :or {a :default-1}}])
    ([{::syms [a b] :or {a :default-2 b :default-3}} a1]))

  (t/deftest multi-arity-with-defaults-test
    ;; intersection for defaults
    (t/is (= {:all      #{`a `b}
              :optional #{`a}}
             (parse #'multi-arity-with-defaults)))))
