(ns darkleaf.di.cache-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(set! *warn-on-reflection* true)

(t/deftest cache-stop-test
  (let [cache  (atom {})
        system (di/start ::root
                         {::root :root}
                         (di/collect-cache cache))]
    (t/is (not (empty? @cache)))
    (di/stop system)
    (t/is (empty? @cache))))

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
  (let [cache (atom {})]
    (with-open [main      (di/start `a
                                    {::param (Object.)}
                                    (di/collect-cache cache))
                secondary (di/start `a
                                    (di/use-cache cache))]
      (t/is (some+identical? @main @secondary)))))

(t/deftest changed-not-identical-test
  (let [cache (atom {})]
    (with-open [main      (di/start `a
                                    {::param (Object.)}
                                    (di/collect-cache cache))
                secondary (di/start `a
                                    (di/use-cache cache)
                                    {::param (Object.)})]
      (t/is (some+not-identical? @main @secondary)))))

(t/deftest changed-equal-and-identical-test
  (let [cache (atom {})]
    (with-open [main      (di/start `a
                                    {::param :equal-and-identical}
                                    (di/collect-cache cache))
                secondary (di/start `a
                                    (di/use-cache cache)
                                    {::param :equal-and-identical})]
      (t/is (some+identical? @main @secondary)))))


(t/deftest changed-equal-but-not-identical-test
  (let [cache (atom {})]
    (with-open [main      (di/start `a
                                    {::param 'equal-but-not-identical}
                                    (di/collect-cache cache))
                secondary (di/start `a
                                    (di/use-cache cache)
                                    {::param 'equal-but-not-identical})]
      (t/is (some+not-identical? main secondary)))))

(t/deftest start-stop-order-test
  (let [cache     (atom {})
        log       (atom [])
        callbacks (fn [system]
                    {:after-build!    (fn [{:keys [key]}]
                                        (swap! log conj [:start system key]))
                     :after-demolish! (fn [{:keys [key]}]
                                        (swap! log conj [:stop system key]))})]
    (with-open [_ (di/start `a
                            {::param :param}
                            (di/collect-cache cache)
                            (di/log (callbacks :main)))
                _ (di/start [::x `a]
                            {::x :x}
                            (di/log (callbacks :second))
                            (di/use-cache cache))
                _ (di/start [::y `a]
                            {::y :y}
                            (di/log (callbacks :third))
                            (di/use-cache cache))])

    (t/is (= [[:start :main   ::param]
              [:start :main   `a]

              [:start :second ::x]
              [:start :second ::di/implicit-root]

              [:start :third  ::y]
              [:start :third  ::di/implicit-root]

              [:stop  :third  ::di/implicit-root]
              [:stop  :third  ::y]

              [:stop  :second ::di/implicit-root]
              [:stop  :second ::x]

              [:stop  :main   `a]
              [:stop  :main   ::param]]
             @log))))

(t/deftest cache-registry-test
  (let [cache (atom {})]
    (with-open [main   (di/start ::root
                                 {::root (di/template [(di/ref ::a) (di/ref ::b)])
                                  ::a    :a
                                  ::b    :b}
                                 (di/update-key ::root conj :c)
                                 (di/collect-cache cache))
                second (di/start ::root
                                 (di/use-cache cache)
                                 {::a "a"})]
      (t/is (= [:a  :b :c] @main))
      (t/is (= ["a" :b :c] @second)))))
