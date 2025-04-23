(ns darkleaf.di.tutorial.x-inspect-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]
   [darkleaf.di.tutorial.x-ns-publics-test :as x-ns-publics-test]))

(t/deftest no-description-test
  (t/is (= [{:key `foo
             #_"NOTE: no description as it is not implemented"}]
           (di/inspect `foo
                       {`foo (reify p/Factory
                               (dependencies [_])
                               (build [_ _] :foo)
                               (demolish [_ _])
                               #_"NOTE: no `p/description implemented")}))))


(t/deftest env-test
  (t/is (= [{:key         "FOO"
             :description {::di/kind :env}}]
           (di/inspect "FOO"))))


(t/deftest fixed-env-test
  (t/is (= [{:key         "FOO"
             :description {::di/kind :trivial
                           :object   "value"}}]
           (di/inspect "FOO" {"FOO" "value"}))))


(def variable :obj)

(t/deftest variable-test
  (t/is (= [{:key         `variable
             :description {::di/kind     :trivial
                           :object       :obj
                           ::di/variable #'variable}}]
           (di/inspect `variable))))


(def variable+factory
  (reify p/Factory
    (dependencies [_])
    (build [_ _] :ok)
    (demolish [_ _])
    #_"NOTE: no `p/description implemented"))

(t/deftest variable+factory-test
  (t/is (= [{:key         `variable+factory
             :description {#_"NOTE: no description as it is not implemented"
                           ::di/variable #'variable+factory}}]
           (di/inspect `variable+factory))))


(def variable+description
  (reify
    p/Factory
    (dependencies [_])
    (build [_ _] :ok)
    (demolish [_ _])
    p/FactoryDescription
    (description [_]
      {::di/kind ::variable+description})))

(t/deftest variable+description-test
  (t/is (= [{:key         `variable+description
             :description {::di/kind     ::variable+description
                           ::di/variable #'variable+description}}]
           (di/inspect `variable+description))))


(def variable+template
  (di/template [42]))

(t/deftest variable+template-test
  (t/is (= [{:key         `variable+template
             :description {::di/kind     :template
                           :template     [42]
                           ::di/variable #'variable+template}}]
           (di/inspect `variable+template))))


(defn component-0-arity
  {::di/kind :component}
  []
  :ok)

(t/deftest component-0-arity-test
  (t/is (= [{:key         `component-0-arity
             :description {::di/kind     :component
                           ::di/variable #'component-0-arity}}]
           (di/inspect `component-0-arity))))


(defn component-1-arity
  {::di/kind :component}
  [-deps]
  :ok)

(t/deftest component-1-arity-test
  (t/is (= [{:key         `component-1-arity
             :description {::di/kind     :component
                           ::di/variable #'component-1-arity}}]
           (di/inspect `component-1-arity))))


(defn service-0-arity
  {::di/kind :service}
  []
  :ok)

(t/deftest service-0-arity-test
  (t/is (= [{:key         `service-0-arity
             :description {::di/kind     :service
                           ::di/variable #'service-0-arity}}]
           (di/inspect `service-0-arity))))


(defn service-n-arity
  {::di/kind :service}
  [-deps]
  :ok)

(t/deftest service-n-arity-test
  (t/is (= [{:key         `service-n-arity
             :description {::di/kind     :service
                           ::di/variable #'service-n-arity}}]
           (di/inspect `service-n-arity))))


(defmulti multimethod-service
  {::di/deps []}
  (fn [-deps kind] kind))

(t/deftest multimethod-service-test
  (t/is (= [{:key         `multimethod-service
             :description {::di/kind     :service
                           ::di/variable #'multimethod-service}}]
           (di/inspect `multimethod-service))))


(t/deftest ref-test
  (t/is (= [{:key          `foo
             :dependencies {`bar :required}
             :description  {::di/kind :ref
                            :key      `bar
                            :type     :required}}
            {:key         `bar
             :description {::di/kind :undefined}}]
           (di/inspect `foo {`foo (di/ref `bar)}))))


(t/deftest template-test
  (t/is (= [{:key          `foo
             :dependencies {`bar :required}
             :description  {::di/kind :template
                            :template [42 (di/ref `bar)]}}
            {:key         `bar
             :description {::di/kind :undefined}}]
           (di/inspect `foo {`foo (di/template [42 (di/ref `bar)])}))))


(t/deftest derive-test
  (t/is (= [{:key          `foo
             :dependencies {`bar :optional}
             :description  {::di/kind :derive
                            :key      `bar
                            :f        str
                            :args     ["arg"]}}
            {:key         `bar
             :description {::di/kind :undefined}}]
           (di/inspect `foo {`foo (di/derive `bar str "arg")}))))


(t/deftest trivial-nil-test
  (t/is (= [{:key         `foo
             :description {::di/kind :trivial
                           :object   nil}}]
           (di/inspect `foo {`foo nil}))))


(t/deftest trivial-obj-test
  (t/is (= [{:key         `foo
             :description {::di/kind :trivial
                           :object   str}}]
           (di/inspect `foo {`foo str}))))


(t/deftest update-key-test
  (t/is (= [{:key          `a
             :dependencies {`a+di-update-key#1-target :optional
                            `a+di-update-key#1-f      :optional
                            `a+di-update-key#1-arg#0  :optional}
             :description  {::di/kind   :middleware
                            :middleware ::di/update-key
                            :target     `a
                            :new-target `a+di-update-key#1-target
                            :f          `a+di-update-key#1-f
                            :args       [`a+di-update-key#1-arg#0]}}
            {:key         `a+di-update-key#1-target
             :description {::di/kind       :trivial
                           :object         :obj
                           ::di/update-key {:target `a
                                            :role   :target}}}
            {:key         `a+di-update-key#1-f
             :description {::di/kind       :trivial
                           :object         str
                           ::di/update-key {:target `a
                                            :role   :f}}}
            {:key         `a+di-update-key#1-arg#0
             :description {::di/kind       :trivial
                           :object         "arg"
                           ::di/update-key {:target `a
                                            :role   :arg}}}]
           (di/inspect `a
                       {`a :obj}
                       (di/update-key `a str "arg")))))


(t/deftest add-side-dependency-test

  ;; нужно inspect переписать, чтобы он ключ сам делал
  ;; и чтобы явно получал implicit-root
  ;; возможно запретить ему быть вектором и мапой

  #_
  (t/is (= [{:key          ::di/implicit-root
             :dependencies {`di/new-key#1 :required
                            `side-dep     :required}
             :description  {::di/kind   :middleware
                            :middleware ::di/add-side-dependency
                            :dep-key    `side-dep}}
            {:key          `di/new-key#1
             :dependencies {`a :required}
             :description  {::di/kind :ref
                            :key      `a
                            :type     :required}}
            {:key         `a
             :description {::di/kind :trivial
                           :object   :obj}}
            {:key         `side-dep
             :description {::di/kind :trivial
                           :object   :side-dep}}]
           (di/inspect `a
                       {`a        :obj
                        `side-dep :side-dep}
                       (di/add-side-dependency `side-dep)))))


(t/deftest ns-publics-test
  (t/is (= [{:key          :ns-publics/darkleaf.di.tutorial.x-ns-publics-test
             :dependencies {`x-ns-publics-test/service   :required
                            `x-ns-publics-test/component :required
                            `x-ns-publics-test/ok-test   :required}
             :description  {::di/kind   :middleware
                            :middleware ::di/ns-publics
                            :ns         'darkleaf.di.tutorial.x-ns-publics-test}}
            {:key          `x-ns-publics-test/service
             :dependencies {`x-ns-publics-test/component :required}
             :description  {::di/kind     :service
                            ::di/variable #'x-ns-publics-test/service}}
            {:key         `x-ns-publics-test/component
             :description {::di/kind     :component
                           ::di/variable #'x-ns-publics-test/component}}
            {:key         `x-ns-publics-test/ok-test
             :description {::di/kind     :trivial
                           :object       x-ns-publics-test/ok-test
                           ::di/variable #'x-ns-publics-test/ok-test}}]
           (di/inspect :ns-publics/darkleaf.di.tutorial.x-ns-publics-test
                       (di/ns-publics)))))


(t/deftest env-parsing-test
  (t/is (= [{:key          :env.long/PORT
             :dependencies {"PORT" :optional}
             :description  {::di/kind   :middleware
                            :middleware ::di/env-parsing
                            :cmap       {:env.long parse-long}}}
            {:key         "PORT"
             :description {::di/kind :trivial
                           :object   "8080"}}]
           (di/inspect :env.long/PORT
                       (di/env-parsing :env.long parse-long)
                       {"PORT" "8080"}))))


(t/deftest log-test
  (t/is (= [{:key         `foo
             :description {::di/kind :trivial
                           :object   :obj
                           ::di/log  {:will-be-logged true
                                      #_#_:opts       nil}}}]
           (di/inspect `foo
                       {`foo :obj}
                       (di/log)))))


(def variable-factory-regression
  (reify p/Factory
    (dependencies [_])
    (build [_ _]
      :ok)
    (demolish [_ _])))

(t/deftest variable-factory-regression-test
  (t/is (= :ok
           @(di/start `variable-factory-regression))))
