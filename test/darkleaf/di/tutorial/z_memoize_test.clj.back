(ns darkleaf.di.tutorial.z-memoize-test ;;todo: name
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn connection
  {::di/stop #(reset! % :stopped)}
  [{url "CONNECTION_URL"}]
  (atom :started))

(defn migrations
  "A long running side effect"
  {::di/kind :component}
  [{connection `connection}]
  #_
  (when (= :stopped @connection)
    (throw (IllegalStateException. "Connection is not started")))
  (random-uuid))

(defn root
  {::di/kind :component}
  [{migrations `migrations}]
  {:migrations migrations})

;; todo: check registry placement

#_
(t/deftest ok
  (let [[cache global-system :as root]
        (di/start [::di/cache `root]
                  {"CONNECTION_URL" "1"}
                  (di/collect-cache))]

    (with-open [local (di/start `root
                                (di/use-cache cache))]
      (t/is (identical? global-system @local)))

    (with-open [local (di/start `root
                                (di/use-cache cache)
                                {"CONNECTION_URL" "1"})]
      (t/is (identical? global-system @local)))

    (with-open [local (di/start `root
                                ;; `use-cache` should be the first registry
                                (di/use-cache cache)
                                {"CONNECTION_URL" "2"})]
      (t/is (not (identical? global-system @local))))


    (di/stop root)

    (t/is (thrown? IllegalStateException
                   (di/start `root
                             (di/use-cache cache))))))
