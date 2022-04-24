(ns darkleaf.di.deps-definition-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn binding-destructuring [{skey  :param
                              qkey  ::param
                              ssym  'param
                              qsym  `param
                              qsym* 'undefined-ns/param
                              str   "param"}]
  [skey qkey qsym qsym* ssym str])

(t/deftest binding-destructuring-test
  (with-open [root (di/start `binding-destructuring
                             {:param              :skey
                              ::param             :qkey
                              'param              :ssym
                              `param              :qsym
                              'undefined-ns/param :qsym*
                              "param"             :str})]
    (t/is (= [:skey :qkey :qsym :qsym* :ssym :str] @root))))


(defn defaults [{skey  :param
                 qkey  ::param
                 ssym  'param
                 qsym  `param
                 qsym* 'undefined-ns/param
                 str   "param"
                 :or   {skey  :skey
                        qkey  :qkey
                        ssym  :ssym
                        qsym  :qsym
                        qsym* :qsym*
                        str   :str}}]
  [skey qkey qsym qsym* ssym str])

(t/deftest defaults-test
  (with-open [root (di/start `defaults)]
    (t/is (= [:skey :qkey :qsym :qsym* :ssym :str] @root))))


(defn keys-destructuring [{:keys              [skey]
                           ::keys             [qkey]
                           :syms              [ssym]
                           ::syms             [qsym]
                           :undefined-ns/syms [qsym*]
                           :strs              [str]}]
  [skey qkey qsym qsym* ssym str])

(t/deftest keys-destructuring-test
  (with-open [root (di/start `keys-destructuring
                             {:skey               :skey
                              ::qkey              :qkey
                              'ssym               :ssym
                              `qsym               :qsym
                              'undefined-ns/qsym* :qsym*
                              "str"               :str})]
    (t/is (= [:skey :qkey :qsym :qsym* :ssym :str] @root))))
