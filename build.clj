(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'org.clojars.darkleaf/di)
(def version "1.0.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s.jar" (name lib)))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]})
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file}))

(defn sync-pom [_]
  (b/copy-file {:src    (str class-dir "/META-INF/maven/org.clojars.darkleaf/di/pom.xml")
                :target "pom.xml"}))

(defn all [_]
  (clean nil)
  (jar nil)
  (sync-pom nil))
