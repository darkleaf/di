(ns darkleaf.di.tutorial.x-inspect-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

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
  (t/is (= [{:key          ::di/implicit-root
             :dependencies {`a :required}
             :description  {:kind :ref
                            :key  `a
                            :type :required}}
            {:key         `a
             :description {:kind     :service
                           :variable #'a}}]
           (di/inspect `a))))


;; todo: name
(t/deftest ok
  (t/is (= [{:key          ::di/implicit-root
             :dependencies {`c :required}
             :description  {:kind :ref
                            :key  `c
                            :type :required}}
            {:key          `c
             :dependencies {`a :required `b :optional}
             :description  {:kind     :service
                            :variable #'c}}
            {:key         `a
             :description {:kind     :service
                           :variable #'a}}
            {:key          `b
             :dependencies {`a :required}
             :description  {:kind     :service
                            :variable #'b}}]
           (di/inspect `c))))


(t/deftest update-key-test
  (t/is (= [{:key          ::di/implicit-root,
             :dependencies {`a :required}
             :description  {:kind :ref
                            :key  `a
                            :type :required}}
            {:key          `a,
             :dependencies {`a+di-update-key#0-target :optional,
                            `a+di-update-key#0-f      :optional}
             :description  {:kind       :middleware
                            :middleware ::di/update-key
                            :target     `a}}
            {:key         `a+di-update-key#0-target
             :description {:kind     :service
                           :variable #'a}}
            {:key         `a+di-update-key#0-f
             :description {:kind   :trivial
                           :object str}}]
           (di/inspect `a (di/update-key `a str)))))

(t/deftest ref-test
  (t/is (= [{:key          :darkleaf.di.core/implicit-root,
             :dependencies {`foo :required}
             :description  {:kind :ref ;; ::di/kind ??
                            :key  `foo
                            :type :required}} ;; frustrated? type and kind
            {:key          `foo
             :dependencies {`bar :required}
             :description  {:kind :ref
                            :key  `bar
                            :type :required}}
            {:key         `bar
             :description {:kind   :trivial
                           :object nil}}]
           (di/inspect `foo {`foo (di/ref `bar)}))))


(t/deftest trivial-nil-test
  (t/is (= [{:key          :darkleaf.di.core/implicit-root,
             :dependencies {`foo :required}
             :description  {:kind :ref
                            :key  `foo
                            :type :required}}
            {:key         `foo
             :description {:kind   :trivial
                           :object nil}}]
           (di/inspect `foo {`foo nil}))))

(t/deftest trivial-obj-test
  (t/is (= [{:key          :darkleaf.di.core/implicit-root,
             :dependencies {`foo :required}
             :description  {:kind :ref
                            :key  `foo
                            :type :required}}
            {:key         `foo
             :description {:kind   :trivial
                           :object str}}]
           (di/inspect `foo {`foo str}))))
