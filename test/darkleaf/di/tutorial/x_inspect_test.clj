(ns darkleaf.di.tutorial.x-inspect-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]
   [darkleaf.di.tutorial.x-ns-publics-test :as x-ns-publics-test]))

(defn implicit-root [key]
  {:key          ::di/implicit-root
   :dependencies {key :required}
   :description  {::di/kind :ref
                  :key      key
                  :type     :required}})


(def variable :obj)

(t/deftest variable-test
  (t/is (= [(implicit-root `variable)
            {:key         `variable
             :description {::di/kind     :trivial
                           :object       :obj
                           ::di/variable #'variable}}]
           (di/inspect `variable))))


(def variable+factory
  (reify p/Factory
    (dependencies [_])
    (build [_ _] :ok)
    (demolish [_ _])))

(t/deftest variable+factory-test
  (t/is (= [(implicit-root `variable+factory)
            {:key         `variable+factory
             :description {::di/kind     :trivial
                           :object       variable+factory
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
  (t/is (= [(implicit-root `variable+description)
            {:key         `variable+description
             :description {::di/kind     ::variable+description
                           ::di/variable #'variable+description}}]
           (di/inspect `variable+description))))


(def variable+template
  (di/template [42]))

(t/deftest variable+template-test
  (t/is (= [(implicit-root `variable+template)
            {:key         `variable+template
             :description {::di/kind     :template
                           :template     [42]
                           ::di/variable #'variable+template}}]
           (di/inspect `variable+template))))


(defn component-0-arity
  {::di/kind :component}
  []
  :ok)

(t/deftest component-0-arity-test
  (t/is (= [(implicit-root `component-0-arity)
            {:key         `component-0-arity
             :description {::di/kind :component
                           :variable #'component-0-arity}}]
           (di/inspect `component-0-arity))))


(defn component-1-arity
  {::di/kind :component}
  [-deps]
  :ok)

(t/deftest component-1-arity-test
  (t/is (= [(implicit-root `component-1-arity)
            {:key         `component-1-arity
             :description {::di/kind :component
                           :variable #'component-1-arity}}]
           (di/inspect `component-1-arity))))


(defn service-0-arity
  {::di/kind :service}
  []
  :ok)

(t/deftest service-0-arity-test
  (t/is (= [(implicit-root `service-0-arity)
            {:key         `service-0-arity
             :description {::di/kind :service
                           :variable #'service-0-arity}}]
           (di/inspect `service-0-arity))))


(defn service-n-arity
  {::di/kind :service}
  [-deps]
  :ok)

(t/deftest service-n-arity-test
  (t/is (= [(implicit-root `service-n-arity)
            {:key         `service-n-arity
             :description {::di/kind :service
                           :variable #'service-n-arity}}]
           (di/inspect `service-n-arity))))


(defmulti multimethod-service
  {::di/deps []}
  (fn [-deps kind] kind))

(t/deftest multimethod-service-test
  (t/is (= [(implicit-root `multimethod-service)
            {:key         `multimethod-service
             :description {::di/kind :service
                           :variable #'multimethod-service}}]
           (di/inspect `multimethod-service))))


(t/deftest ref-test
  (t/is (= [(implicit-root `foo)
            {:key          `foo
             :dependencies {`bar :required}
             :description  {::di/kind :ref
                            :key      `bar
                            :type     :required}}
            {:key         `bar
             :description {::di/kind :trivial
                           :object   nil}}]
           (di/inspect `foo {`foo (di/ref `bar)}))))


(t/deftest template-test
  (t/is (= [(implicit-root `foo)
            {:key          `foo
             :dependencies {`bar :required}
             :description  {::di/kind :template
                            :template [42 (di/ref `bar)]}}
            {:key         `bar
             :description {::di/kind :trivial
                           :object   nil}}]
           (di/inspect `foo {`foo (di/template [42 (di/ref `bar)])}))))


(t/deftest derive-test
  (t/is (= [(implicit-root `foo)
            {:key          `foo
             :dependencies {`bar :optional}
             :description  {::di/kind :derive
                            :key      `bar
                            :f        str
                            :args     ["arg"]}}
            {:key         `bar
             :description {::di/kind :trivial
                           :object   nil}}]
           (di/inspect `foo {`foo (di/derive `bar str "arg")}))))


(t/deftest trivial-nil-test
  (t/is (= [(implicit-root `foo)
            {:key         `foo
             :description {::di/kind :trivial
                           :object   nil}}]
           (di/inspect `foo {`foo nil}))))


(t/deftest trivial-obj-test
  (t/is (= [(implicit-root `foo)
            {:key         `foo
             :description {::di/kind :trivial
                           :object   str}}]
           (di/inspect `foo {`foo str}))))


(t/deftest update-key-test
  (t/is (= [(implicit-root `a)
            {:key          `a
             :dependencies {`a+di-update-key#0-target :optional
                            `a+di-update-key#0-f      :optional
                            `a+di-update-key#0-arg#0  :optional
                            `a+di-update-key#0-arg#1  :optional
                            `a+di-update-key#0-arg#2  :optional}
             :description  {::di/kind       :middleware
                            :middleware     ::di/update-key
                            :target-key     `a
                            :new-target-key `a+di-update-key#0-target
                            :f-key          `a+di-update-key#0-f
                            :f              str
                            :arg-keys       [`a+di-update-key#0-arg#0
                                             `a+di-update-key#0-arg#1
                                             `a+di-update-key#0-arg#2]
                            :args           ["arg" nil (di/ref `b)]}}
            {:key         `a+di-update-key#0-target
             :description {::di/kind :trivial
                           :object   :obj}}
            {:key         `a+di-update-key#0-f
             :description {::di/kind       :trivial
                           :object         str
                           ::di/update-key `a}}
            {:key         `a+di-update-key#0-arg#0
             :description {::di/kind       :trivial
                           :object         "arg"
                           ::di/update-key `a}}
            {:key         `a+di-update-key#0-arg#1
             :description {::di/kind       :trivial
                           :object         nil
                           ::di/update-key `a}}
            {:key          `a+di-update-key#0-arg#2
             :dependencies {`b :required}
             :description  {::di/kind       :ref
                            :key            `b
                            :type           :required
                            ::di/update-key `a}}
            {:key         `b
             :description {::di/kind :trivial
                           :object   :b}}]
           (di/inspect `a
                       {`a :obj
                        `b :b}
                       (di/update-key `a str "arg" nil (di/ref `b))))))


(t/deftest add-side-dependency-test
  (t/is (= [{:key          ::di/implicit-root
             :dependencies {`di/new-key#0 :required
                            `side-dep     :required}
             :description  {::di/kind   :middleware
                            :middleware ::di/add-side-dependency
                            :dep-key    `side-dep}}
            {:key          `di/new-key#0
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
  (t/is (= [(implicit-root :ns-publics/darkleaf.di.tutorial.x-ns-publics-test)
            {:key          :ns-publics/darkleaf.di.tutorial.x-ns-publics-test
             :dependencies {`x-ns-publics-test/service   :required
                            `x-ns-publics-test/component :required
                            `x-ns-publics-test/ok-test   :required}
             :description  {::di/kind   :middleware
                            :middleware ::di/ns-publics
                            :ns         'darkleaf.di.tutorial.x-ns-publics-test}}
            {:key          `x-ns-publics-test/service
             :dependencies {`x-ns-publics-test/component :required}
             :description  {::di/kind :service
                            :variable #'x-ns-publics-test/service}}
            {:key         `x-ns-publics-test/component
             :description {::di/kind :component
                           :variable #'x-ns-publics-test/component}}
            {:key         `x-ns-publics-test/ok-test
             :description {::di/kind     :trivial
                           :object       x-ns-publics-test/ok-test
                           ::di/variable #'x-ns-publics-test/ok-test}}]
           (di/inspect :ns-publics/darkleaf.di.tutorial.x-ns-publics-test
                       (di/ns-publics)))))


(t/deftest env-parsing-test
  (t/is (= [(implicit-root :env.long/PORT)
            {:key          :env.long/PORT
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
  (t/is (= [{:key          ::di/implicit-root
             :dependencies {`foo :required}
             :description  {::di/kind           :ref
                            :key                `foo
                            :type               :required
                            ::di/will-be-logged true}}
            {:key         `foo
             :description {::di/kind           :trivial
                           :object             :obj
                           ::di/will-be-logged true}}]
           (di/inspect `foo
                       {`foo :obj}
                       (di/log)))))


(t/deftest no-description-test
  (t/is (= [(implicit-root `foo)
            {:key `foo
             #_"NOTE: no description as it is not implemented"}]
           (di/inspect `foo
                       {`foo :ok}
                       (fn no-description-middleware [registry]
                         (fn [key]
                           (let [factory (registry key)]
                             (if (= `foo key)
                               (reify
                                 p/Factory
                                 (dependencies [_]
                                   (p/dependencies factory))
                                 (build [_ deps]
                                   (p/build factory deps))
                                 (demolish [_ obj]
                                   (p/demolish factory obj))
                                 #_"NOTE: no `p/description implemented")
                               factory))))))))

(def variable-factory-regression
  (reify p/Factory
    (dependencies [_])
    (build [_ _]
      :ok)
    (demolish [_ _])))

(t/deftest variable-factory-regression-test
  (t/is (= :ok
           @(di/start `variable-factory-regression))))
