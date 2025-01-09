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
;; component 0 arity
;; component 1 arity
;; service 0 arity
;; service N arity
;; service multimethod

(defn a
  {::di/kind :service}
  []
  :ok)

(defn b
  {::di/kind :service}
  [{a `a}]
  :ok)

(defn c
  {::di/kind :service}
  [{a   `a
    b   `b
    :or {b :default}}]
  :ok)


(t/deftest zero-arity-service-test
  (t/is (= [(implicit-root `a)
            {:key         `a
             :description {::di/kind :service
                           :variable #'a}}]
           (di/inspect `a))))


;; todo: name
(t/deftest ok
  (t/is (= [(implicit-root `c)
            {:key          `c
             :dependencies {`a :required `b :optional}
             :description  {::di/kind :service
                            :variable #'c}}
            {:key         `a
             :description {::di/kind :service
                           :variable #'a}}
            {:key          `b
             :dependencies {`a :required}
             :description  {::di/kind :service
                            :variable #'b}}]
           (di/inspect `c))))


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
             :description {::di/kind :service
                           :variable #'a}}
            {:key         `a+di-update-key#0-f
             :description {::di/kind :trivial
                           :object   str}}
            {:key         `a+di-update-key#0-arg#0
             :description {::di/kind :trivial
                           :object   "arg"}}]
           (di/inspect `a (di/update-key `a str "arg")))))
