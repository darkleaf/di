(ns io.github.darkleaf.di.cases.depencencies-test
  (:require [clojure.test :as t]
            [io.github.darkleaf.di.core :as di]))

;;   root
;;  / \
;; a   b
;;  \ /
;;   c

(defn root [{log `log
             a   ::a
             b   ::b}]
  (log :start :root)
  (reify di/Stoppable
    (stop [_]
      (log :stop :root))))

(defn a [{log `log
          c   ::c}]
  (log :start :a)
  (reify di/Stoppable
    (stop [_]
      (log :stop :a))))

(defn b [{log `log
          c   ::c}]
  (log :start :b)
  (reify di/Stoppable
    (stop [_]
      (log :stop :b))))

(defn c [{log `log}]
  (log :start :c)
  (reify di/Stoppable
    (stop [_]
      (log :stop :c))))

(t/deftest order-test
  (let [log    (atom [])
        to-log (fn [action object]
                 (swap! log conj [action object]))
        obj    (di/start ::root {`log to-log})]
    (di/stop obj)
    (t/is (= [[:start :c]
              [:start :a]
              [:start :b]
              [:start :root]

              [:stop :root]
              [:stop :b]
              [:stop :a]
              [:stop :c]]
             @log))))
