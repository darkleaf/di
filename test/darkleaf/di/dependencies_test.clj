(ns darkleaf.di.dependencies-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as dip]))

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

              [`root :stopped]
              [`b :stopped]
              [`a :stopped]
              [`c :stopped]]
             @log))))

(defmacro try-ex-data [& body]
  `(try ~@body
        (catch clojure.lang.ExceptionInfo e#
          (ex-data e#))))

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
            :stack [`missing-path `b-path `root-path ::di/implicit-root]}
           (try-ex-data (di/start `root-path)))))


(defn parent
  [{::syms [missing-key]}]
  :done)


(t/deftest missing-dependency-test
  (t/is (= {:type  ::di/missing-dependency
            :stack [`missing-root ::di/implicit-root]}
           (try-ex-data (di/start `missing-root))))

  (t/is (= {:type  ::di/missing-dependency
            :stack [`missing-key `parent ::di/implicit-root]}
           (try-ex-data (di/start `parent)))))


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
            :stack [`recursion-a `recursion-b `recursion-a ::di/implicit-root]}
           (try-ex-data (di/start `recursion-a))))


  (t/is (= {:type ::di/circular-dependency
            :stack [`recursion-c `recursion-c ::di/implicit-root]}
           (try-ex-data (di/start `recursion-c)))))
