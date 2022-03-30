(ns io.github.darkleaf.di.cases.hooks-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

(defn logging []
  (let [log  (atom [])
        hook (fn [key object]
               (if (fn? object)
                 (fn [& args]
                   (let [result (apply object args)]
                     (swap! log conj [key args result])
                     result))
                 object))]
    [log hook]))

(defn service-a [{} arg]
  [::a arg])

(defn service-b [{a `service-a} arg1 arg2]
  [::b (a arg1) (a arg2)])

(t/deftest logging-hook-test
  (let [[log hook] (logging)]
    (with-open [b (di/start `service-b {} hook)]
      (t/is (= [::b [::a 1] [::a 2]]
               (b 1 2))))
    (t/is (= [[`service-a [1] [::a 1]]
              [`service-a [2] [::a 2]]
              [`service-b [1 2] [::b [::a 1] [::a 2]]]]
             @log))))
