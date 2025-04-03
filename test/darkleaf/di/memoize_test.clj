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
    (let [first  (di/start `a {::param (Object.)} mem)
          second (di/start `a {::param (Object.)} mem)]
      (t/is (some+identical? @first @second)))))


(t/deftest changed-not-identical-test
  (with-open [mem (di/->memoize)]
    (let [first  (di/start `a {::param (Object.)} mem)
          second (di/start `a mem {::param (Object.)})]
      (t/is (some+not-identical? @first @second)))))


(t/deftest changed-equal-and-identical-test
  (with-open [mem (di/->memoize)]
    (let [first  (di/start `a {::param :equal-and-identical} mem)
          second (di/start `a mem {::param :equal-and-identical})]
     (t/is (some+identical? @first @second)))))


(t/deftest changed-equal-but-not-identical-test
  (with-open [mem (di/->memoize)]
    (let [first  (di/start `a {::param 'equal-but-not-identical} mem)
          second (di/start `a mem {::param 'equal-but-not-identical})]
      (t/is (some+identical? @first @second)))))


(t/deftest changed-equal-but-different-test
  (with-open [mem (di/->memoize)]
    (let [first  (di/start `a {::param []} mem)
          second (di/start `a mem {::param '()})]
      (t/is (some+identical? @first @second)))))


(t/deftest start-stop-order-test
  (let [mem       (di/->memoize)
        log       (atom [])
        log-mw    (fn [system key-predicate]
                    (di/log :after-build!
                            #(when (key-predicate (:key %))
                               (swap! log conj [:start system (:key %)]))
                            :after-demolish!
                            #(when (key-predicate (:key %))
                               (swap! log conj [:stop  system (:key %)]))))
        system    (di/start `a
                            {::param :param}
                            (log-mw :system any?)
                            mem)
        _         (di/stop system)
        the-same  (di/start `a
                            {::param :param}
                            (log-mw :the-same any?)
                            mem)
        _         (di/stop the-same)
        redefined (di/start `a
                            {::param :param}
                            (log-mw :redefined any?)
                            mem
                            {::param :new-param}
                            (log-mw :redefined #{::param}))
        _         (di/stop redefined)
        _         (di/stop mem)]
    (t/is (= [[:start :system  ::param]
              [:start :system  `a]
              [:start :system  ::di/implicit-root]

              ;; `the-same` system is fully cached

              [:start :redefined :darkleaf.di.memoize-test/param]
              [:start :redefined  `a]
              [:start :redefined  ::di/implicit-root]

              ;; stopped by (di/stop redefined)
              [:stop  :redefined :darkleaf.di.memoize-test/param]

              ;; stopped by (di/stop mem)
              [:stop  :redefined  ::di/implicit-root]
              [:stop  :redefined  `a]

              [:stop  :system  ::di/implicit-root]
              [:stop  :system  `a]
              [:stop  :system  ::param]]
             @log))))
