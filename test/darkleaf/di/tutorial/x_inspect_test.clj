(ns darkleaf.di.tutorial.x-inspect-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.tutorial.x-ns-publics-test :as x-ns-publics-test]))

(defn implicit-root [key]
  {:key          ::di/implicit-root
   :dependencies {key :required}
   :description  {::di/kind :ref
                  :key      key
                  :type     :required}})


;; todo:
;; service multimethod


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
             :description {::di/kind :trivial  ;; можно попробовать тут что-то другое писать
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
                            `a+di-update-key#0-arg#0  :optional}
             :description  {::di/kind   :middleware
                            :middleware ::di/update-key
                            :target     `a
                            :f          str
                            :args       ["arg"]}}
            {:key         `a+di-update-key#0-target
             :description {::di/kind :trivial
                           :object   :obj}}
            {:key         `a+di-update-key#0-f
             :description {::di/kind :trivial
                           :object   str}}
            {:key         `a+di-update-key#0-arg#0
             :description {::di/kind :trivial
                           :object   "arg"}}]
           (di/inspect `a
                       {`a :obj}
                       (di/update-key `a str "arg")))))


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
             :description {::di/kind :trivial
                           :object   x-ns-publics-test/ok-test}}]
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
             :description  {::di/kind   :ref
                            :key        `foo
                            :type       :required
                            ::di/logged true}}
            {:key         `foo
             :description {::di/kind   :trivial
                           :object     :obj
                           ::di/logged true}}]
           (di/inspect `foo
                       {`foo :obj}
                       (di/log)))))
