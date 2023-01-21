(ns user
  (:require
   [darkleaf.di.core :as di]
   [example.system :as system]))

(defonce root (atom nil))

(defn start []
  (reset! root (di/start ::system/root (system/dev-registry))))

(defn stop []
  (di/stop @root))

;; call them from the repl
(comment
  ;; open http://localhost:8888
  (start)
  (stop)
  nil)
