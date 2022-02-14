(ns io.github.darkleaf.di.cases.ref-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

(t/deftest ref-test
  (with-open [obj (di/start `object
                            {`object     (di/ref `replacement)
                             `replacement ::stub})]
    (t/is (= ::stub @obj))))


(t/deftest ref-n-test
  (with-open [obj (di/start `object
                            {`object (di/ref ::cfg get-in [:a :b :c])
                             ::cfg   {:a {:b {:c ::value}}}})]
    (t/is (= ::value @obj))))


(t/deftest ref-map-test
  (with-open [obj (di/start `object
                            {`object (di/ref-map #{`a ::b "c" :d})
                             `a      1
                             ::b     2
                             "c"     3})]
    (t/is (= {`a 1, ::b 2, "c" 3, :d nil} @obj))))


(t/deftest ref-map-n-test
  (with-open [obj (di/start `object
                            {`object (di/ref-map #{`a ::b "c" :d} assoc :e 4)
                             `a     1
                             ::b    2
                             "c"    3})]
    (t/is (= {`a 1, ::b 2, "c" 3, :d nil, :e 4} @obj))))


(t/deftest ref-form-test
  (let [route-data   (di/ref-form
                      [["/"     {:get {:handler (di/ref `root-handler)}}]
                       ["/news" {:get {:handler (di/ref `news-handler)}}]])
        root-handler (fn [req])
        news-handler (fn [req])]
    (with-open [obj (di/start `route-data
                              {`route-data   route-data
                               `root-handler root-handler
                               `news-handler news-handler})]
      (t/is (= [["/"     {:get {:handler root-handler}}]
                ["/news" {:get {:handler news-handler}}]]
               @obj)))))
