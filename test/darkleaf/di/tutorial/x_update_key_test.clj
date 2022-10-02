(ns darkleaf.di.tutorial.x-update-key-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(def route-data [])

(defn subsystem-a-route-data [-deps]
  ["/a"])

(defn subsystem-b-route-data [-deps]
  ["/b"])

(t/deftest update-key-test
  (with-open [root (di/start `route-data
                             (di/update-key `route-data conj
                                            `subsystem-a-route-data
                                            `subsystem-b-route-data))]
    (t/is (= [["/a"] ["/b"]] @root))))
