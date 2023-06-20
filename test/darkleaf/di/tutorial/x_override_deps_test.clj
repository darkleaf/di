(ns darkleaf.di.tutorial.x-override-deps-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn a [{dep    ::dep
          common ::common}]
  [:a dep common])

(defn b [{dep ::dep
          common ::common}]
  [:b dep common])

(t/deftest ok
  (with-open [root (di/start [`a `b]
                             {::dep-a  :dep-a
                              ::dep-b  :dep-b
                              ::common :c}
                             (di/rename-deps `a {::dep ::dep-a})
                             (di/rename-deps `b {::dep ::dep-b}))]
    (let [[a b] root]
      (t/is (= [:a :dep-a :c] a))
      (t/is (= [:b :dep-b :c] b)))))
