(ns darkleaf.di.cases.exceptions-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest start-test
  (let [root-build-ex (ex-info "build root" {})
        dep-stop-ex   (ex-info "stop dep" {})

        root (reify di/Factory
               (dependencies [_]
                 {`dep true})
               (build [_ deps]
                 (throw root-build-ex)))
        dep  (reify di/Factory
               (dependencies [_]
                 {})
               (build [_ deps]
                 (reify di/Stoppable
                   (stop [_]
                     (throw dep-stop-ex)))))
        ex   (try
               (di/start `root [{`root root, `dep dep}])
               (catch Throwable ex
                 ex))]
    (t/is (= root-build-ex ex))
    (t/is (= [dep-stop-ex] (vec (.getSuppressed ex))))))
