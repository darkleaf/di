(ns io.github.darkleaf.di.cases.var-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

(defn fn-in-var [{dep ::dep, :or {dep ::default}}]
  dep)

(t/deftest fn-in-var-test
  (with-open [s (di/start `fn-in-var {::dep ::value})]
    (t/is (= ::value (s)))))

(t/deftest fn-in-var-default-test
  (with-open [s (di/start `fn-in-var)]
    (t/is (= ::default (s)))))


(def factory-in-var (di/ref ::dep))

(t/deftest factory-in-var-test
  (let [registry {::dep ::value}]
    (with-open [s   (di/start `factory-in-var registry)
                obj (di/start ::factory-in-var registry)]
      (t/is (= ::value @s @obj)))))
