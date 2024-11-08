(ns darkleaf.di.cache-test
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
  (let [cache    (di/->cache)
        registry (fn []
                   [{::param (Object.)}
                    cache])]
    (with-open [main      (di/start `a (registry))
                secondary (di/start `a (registry))]
      (t/is (some+identical? @main @secondary)))))

(t/deftest changed-not-identical-test
  (let [cache    (di/->cache)
        registry (fn []
                   [{::param (Object.)}
                    cache])]
    (with-open [main      (di/start `a (registry))
                secondary (di/start `a (registry)
                                    {::param (Object.)})]
      (t/is (some+not-identical? @main @secondary)))))

(t/deftest changed-equal-and-identical-test
  (let [cache    (di/->cache)
        registry (fn []
                   [{::param :equal-and-identical}
                    cache])]
    (with-open [main      (di/start `a (registry))
                secondary (di/start `a (registry)
                                    {::param :equal-and-identical})]
      (t/is (some+identical? @main @secondary)))))


(t/deftest changed-equal-but-not-identical-test
  (let [cache    (di/->cache)
        registry (fn []
                   [{::param 'equal-but-not-identical}
                    cache])]
    (with-open [main      (di/start `a (registry))
                secondary (di/start `a (registry)
                                    {::param 'equal-but-not-identical})]
      (t/is (some+identical? @main @secondary)))))

(t/deftest changed-equal-but-different-test
  (let [cache    (di/->cache)
        registry (fn []
                   [{::param []}
                    cache])]
    (with-open [main      (di/start `a (registry))
                secondary (di/start `a (registry)
                                    {::param '()})]
      (t/is (some+identical? @main @secondary)))))


(t/deftest start-stop-order-test
  (let [cache     (di/->cache)
        registry  (fn []
                    [{::param :param}
                     cache])
        log       (atom [])
        callbacks (fn [system]
                    {:after-build!    (fn [{:keys [key]}]
                                        (swap! log conj [:start system key]))
                     :after-demolish! (fn [{:keys [key]}]
                                        (swap! log conj [:stop system key]))})]
    (with-open [_ (di/start `a
                            (registry)
                            (di/log (callbacks :main)))
                _ (di/start [::x `a]
                            {::x :x}
                            (di/log (callbacks :second))
                            (registry))
                _ (di/start [::y `a]
                            {::y :y}
                            (di/log (callbacks :third))
                            (registry))])

    (t/is (= [[:start :main   ::param]
              [:start :main   `a]
              [:start :main   ::di/implicit-root]

              [:start :second ::x]
              [:start :second ::di/implicit-root]

              [:start :third  ::y]
              [:start :third  ::di/implicit-root]

              [:stop  :third  ::di/implicit-root]
              [:stop  :third  ::y]

              [:stop  :second ::di/implicit-root]
              [:stop  :second ::x]

              [:stop  :main   ::di/implicit-root]
              [:stop  :main   `a]
              [:stop  :main   ::param]]
             @log))))
