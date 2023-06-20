(ns darkleaf.di.tutorial.x-override-deps-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn a [{dep ::dep}]
  [:a dep])

(defn b [{dep ::dep}]
  [:b dep])

(t/deftest ok
  (with-open [root (di/start [`a `b]
                             {::dep-a :dep-a
                              ::dep-b :dep-b}
                             (di/rename-deps `a {::dep ::dep-a})
                             (di/rename-deps `b {::dep ::dep-b}))]
    (let [[a b] root]
      (t/is (= [:a :dep-a] a))
      (t/is (= [:b :dep-b] b)))))
