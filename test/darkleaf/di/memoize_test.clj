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
                         #(when (and (some? (:object %))
                                     (key-predicate (:key %)))
                            (swap! log conj [:start (:key %)]))
                         :after-demolish!
                         #(when (and (some? (:object %))
                                     (key-predicate (:key %)))
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


;; todo: thrown-with-msg?
(t/deftest should-be-first-test
  (with-open [mem (di/->memoize)]
    (t/is (thrown? RuntimeException
                   (di/start `a {::param 42} mem)))))


(comment

  (require '[clj-async-profiler.core :as prof])
  (prof/serve-ui 8080)


  (prof/profile {}
    (dotimes [_ 1000000]
      (di/start `a {::param 42})))



  (let [mem (di/->memoize {::param 42})]
    (prof/profile {}
      (dotimes [_ 1000000]
        (di/start `a mem))))


  (prof/generate-diffgraph 1 2 {})



  (time
    (dotimes [_ 10000]
      (di/start `a {::param 42})))

  (let [mem (di/->memoize {::param 42})]
    (time
      (dotimes [_ 10000]
        (di/start `a mem))))


 ,,,)
