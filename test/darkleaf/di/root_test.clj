(ns darkleaf.di.root-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]
   [darkleaf.di.protocols :as p]))

(t/deftest auto-closeable-test
  (let [p         (promise)
        stoppable (reify p/Stoppable
                    (stop [_]
                      (deliver p :done)))]
    (with-open [root (di/start ::root {::root stoppable})])
    (t/is (realized? p))
    (t/is (= :done @p))))

(t/deftest stoppable-test
  (let [p         (promise)
        stoppable (reify p/Stoppable
                    (stop [_]
                      (deliver p :done)))]
    (di/stop (di/start ::root {::root stoppable}))
    (t/is (realized? p))
    (t/is (= :done @p))))

(t/deftest ideref-test
  (with-open [root (di/start ::root {::root 42})]
    (t/is (= 42 @root))))

(t/deftest ifn-test
  (with-open [root (di/start
                    ::root
                    {::root (fn
                              ([] 0)
                              ([a1] 1)
                              ([a1 a2] 2)
                              ([a1 a2 a3] 3)
                              ([a1 a2 a3 a4] 4)
                              ([a1 a2 a3 a4 a5] 5)
                              ([a1 a2 a3 a4 a5 a6] 6)
                              ([a1 a2 a3 a4 a5 a6 a7] 7)
                              ([a1 a2 a3 a4 a5 a6 a7 a8] 8)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9] 9)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10] 10)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11] 11)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12] 12)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13] 13)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14] 14)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15] 15)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16] 16)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17] 17)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18] 18)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19] 19)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20] 20)
                              ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 a11 a12 a13 a14 a15 a16 a17 a18 a19 a20 & args] 21))})]
    (t/is (= 0 (.call root)))
    (t/is (= nil (.run root)))
    (t/is (= 0 (root)))
    (t/is (= 1 (root 1)))
    (t/is (= 2 (root 1 2)))
    (t/is (= 3 (root 1 2 3)))
    (t/is (= 4 (root 1 2 3 4)))
    (t/is (= 5 (root 1 2 3 4 5)))
    (t/is (= 6 (root 1 2 3 4 5 6)))
    (t/is (= 7 (root 1 2 3 4 5 6 7)))
    (t/is (= 8 (root 1 2 3 4 5 6 7 8)))
    (t/is (= 9 (root 1 2 3 4 5 6 7 8 9)))
    (t/is (= 10 (root 1 2 3 4 5 6 7 8 9 10)))
    (t/is (= 11 (root 1 2 3 4 5 6 7 8 9 10 11)))
    (t/is (= 12 (root 1 2 3 4 5 6 7 8 9 10 11 12)))
    (t/is (= 13 (root 1 2 3 4 5 6 7 8 9 10 11 12 13)))
    (t/is (= 14 (root 1 2 3 4 5 6 7 8 9 10 11 12 13 14)))
    (t/is (= 15 (root 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15)))
    (t/is (= 16 (root 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16)))
    (t/is (= 17 (root 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17)))
    (t/is (= 18 (root 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18)))
    (t/is (= 19 (root 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19)))
    (t/is (= 20 (root 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20)))
    (t/is (= 21 (root 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21)))
    (t/is (= 2 (apply root 1 [2])))))

(t/deftest pr-test
  (t/is (= "#darkleaf.di.core/root 42"
           (pr-str (di/start ::root {::root 42})))))
