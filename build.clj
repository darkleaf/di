(ns build
  (:require
   [clojure.tools.build.api :as b]
   [clojure.string :as str]))

(def lib 'org.clojars.darkleaf/di)
(def version "3.3.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s.jar" (name lib)))
(def scm-url "git@github.com:darkleaf/di.git")

(defn sha
  [{:keys [dir path] :or {dir "."}}]
  (-> {:command-args (cond-> ["git" "rev-parse" "HEAD"]
                       path (conj "--" path))
       :dir (.getPath (b/resolve-path dir))
       :out :capture}
      b/process
      :out
      str/trim))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     basis
                :src-dirs  ["src"]
                :scm       {:tag                 (sha nil)
                            :connection          (str "scm:git:" scm-url)
                            :developerConnection (str "scm:git:" scm-url)
                            :url                 scm-url}})
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
