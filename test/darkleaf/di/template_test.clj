(ns darkleaf.di.template-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest template-test
  (let [route-data   (di/template
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

(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/template [:a :b :c]"
           (pr-str (di/template [:a :b :c])))))
