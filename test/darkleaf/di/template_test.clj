(ns darkleaf.di.template-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di])
  (:import
   (clojure.lang PersistentQueue)))

(t/deftest template-ref-test
  (t/are [expected tmpl]
      (with-open [root (di/start ::root {::root tmpl ::dep :dep})]
        (t/is (= expected @root)))
    [:dep]
    (di/template [(di/ref ::dep)])

    '(:dep)
    (di/template (list (di/ref ::dep)))

    {:key :dep}
    (di/template {:key (di/ref ::dep)})

    #{:dep}
    (di/template #{(di/ref ::dep)})))

(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/template [:a :b :c]"
           (pr-str (di/template [:a :b :c])))))
