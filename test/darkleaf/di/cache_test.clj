(ns darkleaf.di.cache-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(set! *warn-on-reflection* true)

(t/deftest invalid-cache-test
  (let [[_ cache :as system] (di/start [::root ::di/cache]
                                       {::root :root}
                                       (di/collect-cache))]
    (di/stop system)
    (t/is (thrown? IllegalStateException
                   (di/start ::root (di/use-cache cache))))))

(defn- deep-contains? [coll x]
  (->> coll
       (tree-seq coll? seq)
       (filter #(= x %))
       (first)
       (not= nil)))

(t/deftest not-recursive-test
  (di/with-open [[_ cache] (di/start [::root ::di/cache]
                                     {::root :root}
                                     (di/collect-cache))]
    (t/is (not (deep-contains? @cache cache)))))

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
  (di/with-open [[main cache] (di/start [`a ::di/cache]
                                        {::param (Object.)}
                                        (di/collect-cache))
                 [secondary]  (di/start [`a]
                                        (di/use-cache cache))]
    (t/is (some+identical? main secondary))))

(t/deftest changed-not-identical-test
  (di/with-open [[main cache] (di/start [`a ::di/cache]
                                        {::param (Object.)}
                                        (di/collect-cache))
                 [secondary]  (di/start [`a]
                                        (di/use-cache cache)
                                        {::param (Object.)})]
    (t/is (some+not-identical? main secondary))))

(t/deftest changed-equal-and-identical-test
  (di/with-open [[main cache] (di/start [`a ::di/cache]
                                        {::param :equal-and-identical}
                                        (di/collect-cache))
                 [secondary]  (di/start [`a]
                                        (di/use-cache cache)
                                        {::param :equal-and-identical})]
    (t/is (some+identical? main secondary))))


(t/deftest changed-equal-but-not-identical-test
  (di/with-open [[main cache] (di/start [`a ::di/cache]
                                        {::param 'equal-but-not-identical}
                                        (di/collect-cache))
                 [secondary]  (di/start [`a]
                                        (di/use-cache cache)
                                        {::param 'equal-but-not-identical})]
    (t/is (some+not-identical? main secondary))))

(t/deftest start-stop-order-test
  (let [log       (atom [])
        callbacks (fn [system]
                    {:after-build!    (fn [{:keys [key]}]
                                        (swap! log conj [:start system key]))
                     :after-demolish! (fn [{:keys [key]}]
                                        (swap! log conj [:stop system key]))})]
    (di/with-open [[_ cache] (di/start [`a ::di/cache]
                                       {::param :param}
                                       (di/collect-cache)
                                       (di/log (callbacks :main)))
                   _  (di/start [::x `a]
                                {::x :x}
                                (di/log (callbacks :second))
                                (di/use-cache cache))
                   _  (di/start [::y `a]
                                {::y :y}
                                (di/log (callbacks :third))
                                (di/use-cache cache))])

    (t/is (= [[:start :main   ::param]
              [:start :main   `a]
              [:start :main   ::di/cache]
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
              [:stop  :main   ::di/cache]
              [:stop  :main   `a]
              [:stop  :main   ::param]]
             @log))))
