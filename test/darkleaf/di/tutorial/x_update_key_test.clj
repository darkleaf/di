;; # Update key

^{:nextjournal.clerk/visibility {:code :hide}}
(ns darkleaf.di.tutorial.x-update-key-test
  {:nextjournal.clerk/visibility {:result :hide}}
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; In most cases you just want to instrument or update one dependency.
;; Use `di/update-key` instead of `di/instrument` in this case.

(def route-data [])

(defn subsystem-a-route-data
  {::di/kind :component}
  [-deps]
  ["/a"])

(t/deftest update-key-test
  (with-open [root (di/start `route-data
                             (di/update-key `route-data conj
                                            (di/ref `subsystem-a-route-data)
                                            ["/b"]
                                            nil)
                             {})]
    (t/is (= [["/a"] ["/b"] nil] @root))))
