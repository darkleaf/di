(ns io.github.darkleaf.di.cases.started-test
  (:require
   [clojure.test :as t]
   [io.github.darkleaf.di.core :as di]))

(t/deftest ifn-test
  (with-open [s (di/start
                 `foo
                 {`foo (fn
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
    (t/is (= 0 (.call s)))
    (t/is (= nil (.run s)))
    (t/is (= 0 (s)))
    (t/is (= 1 (s 1)))
    (t/is (= 2 (s 1 2)))
    (t/is (= 3 (s 1 2 3)))
    (t/is (= 4 (s 1 2 3 4)))
    (t/is (= 5 (s 1 2 3 4 5)))
    (t/is (= 6 (s 1 2 3 4 5 6)))
    (t/is (= 7 (s 1 2 3 4 5 6 7)))
    (t/is (= 8 (s 1 2 3 4 5 6 7 8)))
    (t/is (= 9 (s 1 2 3 4 5 6 7 8 9)))
    (t/is (= 10 (s 1 2 3 4 5 6 7 8 9 10)))
    (t/is (= 11 (s 1 2 3 4 5 6 7 8 9 10 11)))
    (t/is (= 12 (s 1 2 3 4 5 6 7 8 9 10 11 12)))
    (t/is (= 13 (s 1 2 3 4 5 6 7 8 9 10 11 12 13)))
    (t/is (= 14 (s 1 2 3 4 5 6 7 8 9 10 11 12 13 14)))
    (t/is (= 15 (s 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15)))
    (t/is (= 16 (s 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16)))
    (t/is (= 17 (s 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17)))
    (t/is (= 18 (s 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18)))
    (t/is (= 19 (s 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19)))
    (t/is (= 20 (s 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20)))
    (t/is (= 21 (s 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21)))
    (t/is (= 2 (apply s 1 [2])))))

(t/deftest pr-test
  (t/is (= "#io.github.darkleaf.di.core/root 42"
           (pr-str (di/start `foo {`foo 42})))))
