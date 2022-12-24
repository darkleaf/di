(ns darkleaf.di.tutorial.p-fmap-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as dip]
   [clojure.spec.alpha :as s]))

;; In some cases, your components may have a complex structure or require transfromation.
;; You can use `di/fmap` to transform a component.

;; The hard way

(defn port [{port "PORT"}]
  (parse-long port))

(t/deftest port-test
  (with-open [root (di/start `port {"PORT" "8080"})]
    (t/is (= 8080 @root))))


;; The easy way

(def port' (-> (di/ref "PORT")
               (di/fmap parse-long)))

(t/deftest port'-test
  (with-open [root (di/start `port' {"PORT" "8080"})]
    (t/is (= 8080 @root))))


(def box (-> (di/template [(di/opt-ref ::a)
                           (di/opt-ref ::b)
                           (di/opt-ref ::c)])
             (di/fmap (partial filter some?))))

(t/deftest box-test
  (with-open [root (di/start `box {::b :b})]
    (t/is (= [:b] @root))))

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
