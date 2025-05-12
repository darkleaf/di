(ns darkleaf.di.template-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p])
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
    (di/template #{(di/ref ::dep)})

    [:dep]
    (di/template [(reify p/Factory
                    (dependencies [_])
                    (build [_ deps _] :dep)
                    (description [_]))])))

(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/template [:a :b :c]"
           (pr-str (di/template [:a :b :c])))))
