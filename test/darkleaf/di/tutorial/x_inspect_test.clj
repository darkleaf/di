(ns darkleaf.di.tutorial.x-inspect-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]
   [darkleaf.di.tutorial.x-ns-publics-test :as x-ns-publics-test]))

(t/deftest env-test
  (t/is (= [{:key         "FOO"
             :description {::di/kind :env
                           ::di/root true}}]
           (di/inspect "FOO"))))


(t/deftest fixed-env-test
  (t/is (= [{:key         "FOO"
             :description {::di/kind :trivial
                           :object   "value"
                           ::di/root true}}]
           (di/inspect "FOO" {"FOO" "value"}))))


(def variable :obj)

(t/deftest variable-test
  (t/is (= [{:key         `variable
             :description {::di/kind     :trivial
                           :object       :obj
                           ::di/root     true
                           ::di/variable #'variable}}]
           (di/inspect `variable))))


(def variable+factory
  (reify p/Factory
    (dependencies [_])
    (build [_ _ _] :ok)
    (description [_] {})))

(t/deftest variable+factory-test
  (t/is (= [{:key         `variable+factory
             :description {::di/root     true
                           ::di/variable #'variable+factory}}]
           (di/inspect `variable+factory))))


(def variable+description
  (reify p/Factory
    (dependencies [_])
    (build [_ _ _] :ok)
    (description [_]
      {::di/kind ::variable+description})))

(t/deftest variable+description-test
  (t/is (= [{:key         `variable+description
             :description {::di/kind     ::variable+description
                           ::di/root     true
                           ::di/variable #'variable+description}}]
           (di/inspect `variable+description))))


(def variable+template
  (di/template [42]))

(t/deftest variable+template-test
  (t/is (= [{:key         `variable+template
             :description {::di/kind     :template
                           :template     [42]
                           ::di/root     true
                           ::di/variable #'variable+template}}]
           (di/inspect `variable+template))))


(defn component-0-arity
  {::di/kind :component}
  []
  :ok)

(t/deftest component-0-arity-test
  (t/is (= [{:key         `component-0-arity
             :description {::di/kind     :component
                           ::di/root     true
                           ::di/variable #'component-0-arity}}]
           (di/inspect `component-0-arity))))


(defn component-1-arity
  {::di/kind :component}
  [-deps]
  :ok)

(t/deftest component-1-arity-test
  (t/is (= [{:key         `component-1-arity
             :description {::di/kind     :component
                           ::di/root     true
                           ::di/variable #'component-1-arity}}]
           (di/inspect `component-1-arity))))


(defn service-0-arity
  {::di/kind :service}
  []
  :ok)

(t/deftest service-0-arity-test
  (t/is (= [{:key         `service-0-arity
             :description {::di/kind     :service
                           ::di/root     true
                           ::di/variable #'service-0-arity}}]
           (di/inspect `service-0-arity))))


(defn service-n-arity
  {::di/kind :service}
  [-deps]
  :ok)

(t/deftest service-n-arity-test
  (t/is (= [{:key         `service-n-arity
             :description {::di/kind     :service
                           ::di/root     true
                           ::di/variable #'service-n-arity}}]
           (di/inspect `service-n-arity))))


(defmulti multimethod-service
  {::di/deps []}
  (fn [-deps kind] kind))

(t/deftest multimethod-service-test
  (t/is (= [{:key         `multimethod-service
             :description {::di/kind     :service
                           ::di/root     true
                           ::di/variable #'multimethod-service}}]
           (di/inspect `multimethod-service))))


(t/deftest ref-test
  (t/is (= [{:key          `foo
             :dependencies {`bar :required}
             :description  {::di/kind :ref
                            :key      `bar
                            :type     :required
                            ::di/root true}}
            {:key         `bar
             :description {::di/kind :undefined}}]
           (di/inspect `foo {`foo (di/ref `bar)}))))


(t/deftest template-test
  (t/is (= [{:key          `foo
             :dependencies {`bar :required}
             :description  {::di/kind :template
                            :template [42 (di/ref `bar)]
                            ::di/root true}}
            {:key         `bar
             :description {::di/kind :undefined}}]
           (di/inspect `foo {`foo (di/template [42 (di/ref `bar)])}))))


(t/deftest derive-test
  (t/is (= [{:key          `foo
             :dependencies {`bar :optional}
             :description  {::di/kind :derive
                            :key      `bar
                            :f        str
                            :args     ["arg"]
                            ::di/root true}}
            {:key         `bar
             :description {::di/kind :undefined}}]
           (di/inspect `foo {`foo (di/derive `bar str "arg")}))))


(t/deftest trivial-nil-test
  (t/is (= [{:key         `foo
             :description {::di/kind :trivial
                           :object   nil
                           ::di/root true}}]
           (di/inspect `foo {`foo nil}))))


(t/deftest trivial-obj-test
  (t/is (= [{:key         `foo
             :description {::di/kind :trivial
                           :object   str
                           ::di/root true}}]
           (di/inspect `foo {`foo str}))))


(t/deftest update-key-test
  (t/is (= [{:key          `a
             :dependencies (seq {`b :required})
             :description  {::di/kind :trivial
                            :object   :obj
                            ::di/root true}}
            {:key         `b
             :description {::di/kind :trivial
                           :object   "b"}}]
           (di/inspect `a
                       {`a :obj
                        `b "b"}
                       (di/update-key `a str (di/ref `b))
                       (di/update-key `a identity)))))


(t/deftest add-side-dependency-test
  (t/is (= [{:key         `a
             :description {::di/kind :trivial
                           :object   :obj
                           ::di/root true}}
            {:key         `side-dep-1
             :description {::di/kind            :trivial
                           :object              :side-dep
                           ::di/side-dependency true}}
            {:key         `side-dep-2
             :description {::di/kind            :trivial
                           :object              :side-dep
                           ::di/side-dependency true}}]
           (di/inspect `a
                       {`a          :obj
                        `side-dep-1 :side-dep
                        `side-dep-2 :side-dep}
                       (di/add-side-dependency `side-dep-1)
                       (di/add-side-dependency `side-dep-2)))))


(t/deftest ns-publics-test
  (t/is (= [{:key          :ns-publics/darkleaf.di.tutorial.x-ns-publics-test
             :dependencies {`x-ns-publics-test/service   :required
                            `x-ns-publics-test/component :required
                            `x-ns-publics-test/ok-test   :required}
             :description  {::di/kind   :middleware
                            :middleware ::di/ns-publics
                            :ns         'darkleaf.di.tutorial.x-ns-publics-test
                            ::di/root   true}}
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
                            :cmap       {:env.long parse-long}
                            ::di/root   true}}
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
                                      #_#_:opts       nil}
                           ::di/root true}}]
           (di/inspect `foo
                       {`foo :obj}
                       (di/log)))))


(def variable-factory-regression
  (reify p/Factory
    (dependencies [_])
    (build [_ _ _] :ok)
    (description [_])))

(t/deftest variable-factory-regression-test
  (t/is (= :ok
           @(di/start `variable-factory-regression))))


(t/deftest vector-test
  (t/is (= [{:key "A"
             :description {::di/kind :env
                           ::di/root true}}
            {:key "B"
             :description {::di/kind :env
                           ::di/root true}}]
           (di/inspect ["A" "B"]))))


(t/deftest map-test
  (t/is (= [{:key         "A"
             :description {::di/kind :env
                           ::di/root true}}
            {:key         "B"
             :description {::di/kind :env
                           ::di/root true}}]
           (di/inspect {:a "A" :b "B"}))))
