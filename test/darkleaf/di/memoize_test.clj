(ns darkleaf.di.memoize-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.utils :refer [catch-some]]))

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


(t/deftest changed-not-identical-test
  (with-open [mem    (di/->memoize    {::param (Object.)})
              first  (di/start `a mem)
              second (di/start `a mem {::param (Object.)})]
    (t/is (some+not-identical? @first @second))))


(t/deftest changed-equal-and-identical-test
  (with-open [mem    (di/->memoize    {::param :equal-and-identical})
              first  (di/start `a mem)
              second (di/start `a mem {::param :equal-and-identical})]
    (t/is (some+identical? @first @second))))


(t/deftest changed-equal-but-not-identical-test
  (with-open [mem    (di/->memoize    {::param 'equal-but-not-identical})
              first  (di/start `a mem)
              second (di/start `a mem {::param 'equal-but-not-identical})]
    (t/is (some+identical? @first @second))))


(t/deftest changed-equal-but-different-test
  (with-open [mem    (di/->memoize    {::param []})
              first  (di/start `a mem)
              second (di/start `a mem {::param '()})]
    (t/is (some+identical? @first @second))))

(t/deftest start-stop-order-test
  (let [log    (atom [])
        log-mw (fn [key-predicate]
                 (di/log :after-build!
                         #(when (key-predicate (:key %))
                            (swap! log conj [:start (:key %)]))
                         :after-demolish!
                         #(when (key-predicate (:key %))
                            (swap! log conj [:stop  (:key %)]))))
        mem    (di/->memoize {::param :param} (log-mw any?))]
    (-> (di/start `a mem)
        (di/stop))
    (t/is (= [[:start ::param]
              [:start `a]]
             @log))
    (swap! log empty)

    (-> (di/start `a mem)
        (di/stop))
    (t/is (= [] @log))

    (-> (di/start `a mem
                  {::param :new-param}
                  (log-mw #{::param}))
        (di/stop))
    (t/is (= [[:start ::param]
              [:start `a]
              [:stop ::param]]
             @log))
    (swap! log empty)

    (di/stop mem)
    (t/is (= [[:stop `a]
              [:stop `a]
              [:stop ::param]]
             @log))
    (swap! log empty)

    (-> (di/start `a mem)
        (di/stop))
    (t/is (= [[:start ::param]
              [:start `a]]
             @log))))

(t/deftest should-be-first-test
  (with-open [mem (di/->memoize)]
    (let [ex (catch-some (di/start `a {::param 42} mem))]
      (t/is (= ::di/wrong-memoized-registry-position
               (-> ex ex-data :type))))))


(t/deftest service-deps+body-change-test
  (with-open [mem (di/->memoize {::param-1 42
                                 ::param-2 0})]

    (defn service-1 [{param ::param-1}]
      [:a param])
    (with-open [s (di/start `service-1 mem)]
      (t/is (= [:a 42] (s))))

    (defn service-1 [{param ::param-2}]
      [:b param])
    (with-open [s (di/start `service-1 mem)]
      (t/is (= [:b 0] (s))))))


(t/deftest component-deps+body-change-test
  (with-open [mem (di/->memoize {::param-1 42
                                 ::param-2 0})]

    (defn component-1
      {::di/kind :component}
      [{param ::param-1}]
      [:a param])
    (with-open [s (di/start `component-1 mem)]
      (t/is (= [:a 42] @s)))

    (defn component-1
      {::di/kind :component}
      [{param ::param-2}]
      [:b param])
    (with-open [s (di/start `component-1 mem)]
      (t/is (= [:b 0] @s)))))


(t/deftest var-type-change-test
  (with-open [mem (di/->memoize {::param 42})]

    (def var-type-change-var :just-value)
    (with-open [s (di/start `var-type-change-var mem)]
      (t/is (= :just-value @s)))

    (defn var-type-change-var
      {::di/kind :component}
      [{param ::param}]
      [:a param])
    (with-open [s (di/start `var-type-change-var mem)]
      (t/is (= [:a 42] @s)))))


(t/deftest remove-watch-test
  (def remove-watch-var :_)
  (with-open [mem (di/->memoize)
              s   (di/start `remove-watch-var mem)])
  (t/is (= {} (.getWatches #'remove-watch-var))))



(t/deftest invalidation-log-test
  (let [log    (atom [])
        log-mw (di/log :after-build!
                       #(swap! log conj [:start (:key %)])
                       :after-demolish!
                       #(swap! log conj [:stop  (:key %)]))
        mem    (di/->memoize {::param :param} log-mw)]

    (defn invalidation-a []
      :a)

    (defn invalidation-b [{a `invalidation-a}]
      (a))

    (-> (di/start `invalidation-b mem)
        (di/stop))
    (t/is (= [[:start `invalidation-a]
              [:start `invalidation-b]]
             @log))
    (swap! log empty)

    (-> (di/start `invalidation-b mem)
        (di/stop))
    (t/is (= [] @log))


    (defn invalidation-a []
      :a')

    ;; A service without arguments is just a var
    ;; so `invalidation-b` received the same arguments.
    (-> (di/start `invalidation-b mem)
        (di/stop))
    (t/is (= [[:start `invalidation-a]]
             @log))
    (swap! log empty)


    (defn invalidation-a [{param ::param}]
      :a'')

    (-> (di/start `invalidation-b mem)
        (di/stop))
    (t/is (= [[:start ::param]
              [:start `invalidation-a]
              [:start `invalidation-b]]
             @log))
    (swap! log empty)


    (di/stop mem)
    (t/is (= [[:stop `invalidation-b]
              [:stop `invalidation-a]
              [:stop ::param]
              [:stop `invalidation-a]
              [:stop `invalidation-b]
              [:stop `invalidation-a]]
             @log))))

(comment

  (require '[clj-async-profiler.core :as prof])
  (prof/serve-ui 8080)


  (def N 10000)

  (prof/profile {}
    (dotimes [_ N]
      (di/start `a {::param 42})))



  (let [mem (di/->memoize {::param 42})]
    (prof/profile {}
      (dotimes [_ N]
        (di/start `a mem))))


  (prof/generate-diffgraph 1 2 {})



  (time
    (dotimes [_ N]
      (di/start `a {::param 42})))

  (let [mem (di/->memoize {::param 42})]
    (time
      (dotimes [_ N]
        (di/start `a mem))))


 ,,,)
