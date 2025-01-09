(ns darkleaf.di.tutorial.x-inspect-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

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

#_
(t/deftest add-side-dependency-test
  (t/is (= []
           (di/inspect `a (di/add-side-dependency)))))
