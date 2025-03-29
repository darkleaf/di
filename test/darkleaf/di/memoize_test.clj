(ns darkleaf.di.memoize-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(set! *warn-on-reflection* true)

(t/deftest memoize-test
  (let [a         'a
        identity* (memoize identity)]
    (identity* a)
    (t/is (not (identical? a  (identity  'a))))
    (t/is      (identical? a  (identity* 'a)))))

(defn- some+identical? [a b]
  (and (some? a)
       (some? b)
       (identical? a b)))

(defn- some+not-identical? [a b]
  (and (some? a)
       (some? b)
       (not (identical? a b))))

(defn a
  {::di/kind :component}
  [{_ ::param}]
  (Object.))

(t/deftest ok-test
  (with-open [mem (di/->memoize)]
    (let [registry  (fn []
                      [{::param (Object.)}
                       mem])
          main      (di/start `a (registry))
          secondary (di/start `a (registry))]
      (t/is (some+identical? @main @secondary)))))

(t/deftest changed-not-identical-test
  (with-open [mem (di/->memoize)]
    (let [registry  (fn []
                      [{::param (Object.)}
                       mem])
          main      (di/start `a (registry))
          secondary (di/start `a (registry)
                              {::param (Object.)})]
      (t/is (some+not-identical? @main @secondary)))))

(t/deftest changed-equal-and-identical-test
  (with-open [mem (di/->memoize)]
    (let [registry  (fn []
                      [{::param :equal-and-identical}
                       mem])
          main      (di/start `a (registry))
          secondary (di/start `a (registry)
                              {::param :equal-and-identical})]
     (t/is (some+identical? @main @secondary)))))


(t/deftest changed-equal-but-not-identical-test
  (with-open [mem (di/->memoize)]
    (let [registry  (fn []
                      [{::param 'equal-but-not-identical}
                       mem])
          main      (di/start `a (registry))
          secondary (di/start `a (registry)
                              {::param 'equal-but-not-identical})]
      (t/is (some+identical? @main @secondary)))))

(t/deftest changed-equal-but-different-test
  (with-open [mem (di/->memoize)]
    (let [registry  (fn []
                      [{::param []}
                       mem])
          main      (di/start `a (registry))
          secondary (di/start `a (registry)
                              {::param '()})]
      (t/is (some+identical? @main @secondary)))))


(t/deftest start-stop-order-test
  (let [mem       (di/->memoize)
        log       (atom [])
        callbacks (fn [system]
                    {:after-build!    (fn [{:keys [key]}]
                                        (swap! log conj [:start system key]))
                     :after-demolish! (fn [{:keys [key]}]
                                        (swap! log conj [:stop system key]))})
        registry  (fn [system]
                    [{::param :param}
                     (di/log (callbacks system))
                     mem])]
    (di/start `a
              (registry :first))
    (di/start [::x `a]
              {::x :x}
              (registry :second))
    (di/start [::y `a]
              {::y :y}
              (registry :third))
    ;; (di/stop mem)
    (.close mem)

    #_(prn @mem)


    (t/is (= [[:start :first  ::param]
              [:start :first  `a]
              [:start :first  ::di/implicit-root]

              [:start :second ::x]
              [:start :second ::di/implicit-root]

              [:start :third  ::y]
              [:start :third  ::di/implicit-root]

              [:stop  :third  ::di/implicit-root]
              [:stop  :third  ::y]

              [:stop  :second ::di/implicit-root]
              [:stop  :second ::x]

              [:stop  :first  ::di/implicit-root]
              [:stop  :first  `a]
              [:stop  :first  ::param]]
             @log))))
