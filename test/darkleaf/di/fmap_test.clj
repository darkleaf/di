(ns darkleaf.di.fmap-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as dip]))

;; todo: rewrite
(t/deftest stoppable-test
  (let [*log (atom [])]
    (with-open [root (di/start ::root {::root (di/fmap (reify dip/Factory
                                                         (dependencies [_])
                                                         (build [_ _]
                                                           (reify dip/Stoppable
                                                             (unwrap [_]
                                                               :a)
                                                             (stop [_]
                                                               (swap! *log conj :a)))))
                                                       (fn [a]
                                                         [:fmap a]))})]
      (t/is (= [:fmap :a] @root)))
    (t/is (= [:a] @*log))))
