(ns darkleaf.di.template-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest template-ref-test
  (t/are [expected actual]
      (with-open [root (di/start `root {`root actual ::dep :dep})]
        (t/is (= expected @root)))
    [:dep]
    (di/template [(di/ref ::dep)])

    '(:dep)
    (di/template (list (di/ref ::dep)))

    {:key :dep}
    (di/template {:key (di/ref ::dep)})

    #{:dep}
    (di/template #{(di/ref ::dep)})))


(def dep :dep)

(def template (di/template [#'dep]))

(t/deftest template-var-test
  (with-open [root (di/start `template)]
    (t/is (= [:dep] @root))))


(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/template [:a :b :c]"
           (pr-str (di/template [:a :b :c])))))
