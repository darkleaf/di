(ns darkleaf.di.depencencies-test
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
  {::di/stop #(swap! % conj [`root :stopped])}
  [{a `a, b `b, log ::log}]
  (swap! log conj [`root :built])
  log)

(defn a
  {::di/stop #(swap! % conj [`a :stopped])}
  [{c `c, log ::log}]
  (swap! log conj [`a :built])
  log)

(defn b
  {::di/stop #(swap! % conj [`b :stopped])}
  [{c `c, log ::log}]
  (swap! log conj [`b :built])
  log)

(defn c
  {::di/stop #(swap! % conj [`c :stopped])}
  [{log ::log}]
  (swap! log conj [`c :built])
  log)

(t/deftest order-test
  (let [log (atom [])]
    (-> (di/start `root {::log log})
        (di/stop))
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
            :stack [`missing-path `b-path `root-path]}
           (-> (di/start `root-path)
               try-ex-data
               (select-keys [:type :stack])))))

(defn parent
  [{::syms [missing-key]}]
  :done)


(t/deftest missing-dependency-test
  (t/is (= {:type  ::di/missing-dependency
            :stack [`missing-root]}
           (-> (di/start `missing-root)
               try-ex-data
               (select-keys [:type :stack]))))

  (t/is (= {:type ::di/missing-dependency
            :stack [`missing-key `parent]}
           (-> (di/start `parent)
               try-ex-data
               (select-keys [:type :stack])))))


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
               try-ex-data
               (select-keys [:type :stack]))))

  (t/is (= {:type ::di/circular-dependency
            :stack [`recursion-c `recursion-c]}
           (-> (di/start `recursion-c)
               try-ex-data
               (select-keys [:type :stack])))))
