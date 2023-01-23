(ns user
  (:require
   [nextjournal.clerk :as clerk]))

(comment
  (clerk/serve! {:browse? true :watch-paths ["notebooks" "test"]})
  (clerk/halt!)

  (clerk/show! "notebooks/index.clj")

  (clerk/show! 'darkleaf.di.tutorial.a-intro-test)



  (clerk/build! {:index "notebooks/index.clj"
                 :paths ["notebooks/*.clj" "test/**/*.clj"]})
  nil)
