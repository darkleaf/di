(ns darkleaf.di.dependencies-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as dip]
   [darkleaf.di.utils :refer [catch-some]]))

;;   root
;;  / \
;; a   b
;;  \ /
;;   c

(defn root
  {::di/kind :component}
  [{a `a, b `b}]
  :root)

(defn a
  {::di/kind :component}
  [{c `c}]
  :a)

(defn b
  {::di/kind :component}
  [{c `c}]
  :b)

(defn c
  {::di/kind :component}
  []
  :c)

(t/deftest order-test
  (let [log             (atom [])
        after-build!    (fn [{:keys [key]}]
                          (swap! log conj [key :built]))
        after-demolish! (fn [{:keys [key]}]
                          (swap! log conj [key :stopped]))]
    (with-open [root (di/start `root (di/log :after-build!    after-build!
                                             :after-demolish! after-demolish!))])
    (t/is (= [[`c :built]
              [`a :built]
              [`b :built]
              [`root :built]
              [::di/implicit-root :built]

              [::di/implicit-root :stopped]
              [`root :stopped]
              [`b :stopped]
              [`a :stopped]
              [`c :stopped]]
             @log))))

(defn root-path
  [{::syms [a-path b-path c-path]}]
  :done)

(defn final-path
  [{}]
  :done)

(defn a-path
  [{::syms [final-path]}]
  :done)

(defn b-path
  [{::syms [missing-path]}]
  :done)

(defn c-path
  [{::syms [final-path]}]
  :done)

(t/deftest error-path-test
  (t/is (= {:type ::di/missing-dependency
            :stack [`missing-path `b-path `root-path]}
           (-> (di/start `root-path)
               catch-some
               ex-data))))


(defn parent
  [{::syms [missing-key]}]
  :done)


(t/deftest missing-dependency-test
  (t/is (= {:type  ::di/missing-dependency
            :stack [`missing-root]}
           (-> (di/start `missing-root)
               catch-some
               ex-data)))

  (t/is (= {:type  ::di/missing-dependency
            :stack [`missing-key `parent]}
           (-> (di/start `parent)
               catch-some
               ex-data))))


(defn recursion-a
  [{::syms [recursion-b]}]
  :done)

(defn recursion-b
  [{::syms [recursion-a]}]
  :done)

(defn recursion-c
  [{::syms [recursion-c]}]
  :done)

(t/deftest circular-dependency-test
  (t/is (= {:type ::di/circular-dependency
            :stack [`recursion-a `recursion-b `recursion-a]}
           (-> (di/start `recursion-a)
               catch-some
               ex-data)))



  (t/is (= {:type ::di/circular-dependency
            :stack [`recursion-c `recursion-c]}
           (-> (di/start `recursion-c)
               catch-some
               ex-data))))
