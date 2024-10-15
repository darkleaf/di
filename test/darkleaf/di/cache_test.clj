(ns darkleaf.di.cache-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(set! *warn-on-reflection* true)


;; мне надоело логгеры писать
;; может уже написать реестр, который логирует все действия?
;; только его нужно передавать извне
;; (atom [ [`key :started] [`key :stopped] ] )
(defn a
  {::di/kind :component}
  ;; {::di/stop #(swap! (:log %))}

  []
  [{log ::log}]
  {:name :a
   :obj  (Object.)})

(t/deftest a-test
  (di/with-open [[main cache] (di/start [`a ::di/cache]
                                        (di/collect-cache))
                 [secondary]  (di/start [`a]
                                        (di/use-cache cache))]
    (t/is (not= nil   main secondary))
    (t/is (identical? main secondary))))

(defn b
  {::di/kind :component}
  [{conf "B_CONF"}]
  {:name :b
   :conf conf
   :obj  (Object.)})

(t/deftest b-test
  (di/with-open [[main cache] (di/start [`b ::di/cache]
                                        {"B_CONF" "conf"}
                                        ;; должен быть последним в цепочке, чтобы закешировать все
                                        (di/collect-cache))
                 [secondary]  (di/start [`b]
                                        (di/use-cache cache))]
    (t/is (not= nil   main secondary))
    (t/is (identical? main secondary))))

(t/deftest b-changed-test
  (di/with-open [[main cache] (di/start [`b ::di/cache]
                                        {"B_CONF" "conf"}
                                        (di/collect-cache))
                 [secondary]  (di/start [`b]
                                        ;; должен быть первым, чтобы его можно было переопределять
                                        (di/use-cache cache)
                                        {"B_CONF" "changed"})]
    (t/is (not= nil        main secondary))
    (t/is (not (identical? main secondary)))))

(defn c
  {::di/kind :component}
  [{a `a, b `b}]
  {:name :c
   :a    a
   :b    b
   :obj  (Object.)})

(t/deftest c-test
  (di/with-open [[main cache] (di/start [`c ::di/cache]
                                        {"B_CONF" "conf"}
                                        (di/collect-cache))
                 [secondary]  (di/start [`c]
                                        (di/use-cache cache))]
    (t/is (not= nil   main secondary))
    (t/is (identical? main secondary))))

(t/deftest c-changed-test
  (di/with-open [[main cache] (di/start [`c ::di/cache]
                                        {"B_CONF" "conf"}
                                        (di/collect-cache))
                 [secondary]  (di/start [`c]
                                        (di/use-cache cache)
                                        {"B_CONF" "changed"})]
    (t/is (not= nil        main secondary))
    (t/is (not (identical? main secondary)))
    (t/is (= :c
             (:name main)
             (:name secondary)))
    (t/is (identical? (:a main)
                      (:a secondary)))
    (t/is (not (identical? (:b main)
                           (:b secondary))))
    ;; надо ли проверять?
    (t/is (not (identical? (:obj main)
                           (:obj secondary))))))

(t/deftest invalid-cache-test
  (let [[main cache :as system] (di/start [`c ::di/cache]
                                          {"B_CONF" "conf"}
                                          (di/collect-cache))
        _    (di/stop system)]
    (t/is (thrown? IllegalStateException
                   (di/start `c
                             (di/use-cache cache))))))

(t/deftest not-recursive-test
  (di/with-open [[main cache] (di/start [`c ::di/cache]
                                        {"B_CONF" "conf"}
                                        (di/collect-cache))]
    (t/try-expr "must not be recrusive"
                (prn-str cache))))
