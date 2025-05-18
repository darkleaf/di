(ns darkleaf.di.tutorial.x-override-deps-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(defn a
  {::di/kind :component}
  [{dep    ::dep
    common ::common}]
  [:a dep common])

(defn b
  {::di/kind :component}
  [{dep ::dep
    common ::common}]
  [:b dep common])

(t/deftest ok
  (with-open [root (di/start [`a `b]
                             {::dep-a  :dep-a
                              ::common :c}
                             (di/redefine-deps `a ::dep (di/ref ::dep-a))
                             (di/redefine-deps `b ::dep :dep-b))]
    (let [[a b] root]
      (t/is (= [:a :dep-a :c] a))
      (t/is (= [:b :dep-b :c] b)))))
