(ns darkleaf.di.add-side-dependency-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

(t/deftest bug--array-map->hash-map--prepare
  (t/is (= clojure.lang.PersistentArrayMap
           (class {:a 1
                   :b 2
                   :c 3
                   :d 4
                   :e 5
                   :f 6
                   :g 7
                   :h 8})))
  (t/is (= clojure.lang.PersistentHashMap
           (class {:a 1
                   :b 2
                   :c 3
                   :d 4
                   :e 5
                   :f 6
                   :g 7
                   :h 8
                   :i 9}))))

(defn- a
  {::di/kind :component}
  []
  :a)

(defn- b
  {::di/kind :component}
  []
  :b)

(defn- c
  {::di/kind :component}
  []
  :c)

(defn- d
  {::di/kind :component}
  []
  :d)

(defn- e
  {::di/kind :component}
  []
  :e)

(defn- f
  {::di/kind :component}
  []
  :f)

(defn- g
  {::di/kind :component}
  []
  :g)

(defn- h
  {::di/kind :component}
  []
  :h)

(defn- side-dep
  {::di/kind :component}
  []
  :side-dep)

(t/deftest bug-array-map->hash-map
  (let [log          (atom [])
        after-build! (fn [{:keys [key]}]
                       (swap! log conj key))]
    (with-open [root (di/start ::root
                               {::root (di/template
                                        {:a (di/ref `a)
                                         :b (di/ref `b)
                                         :c (di/ref `c)
                                         :d (di/ref `d)
                                         :e (di/ref `e)
                                         :f (di/ref `f)
                                         :g (di/ref `g)
                                         :h (di/ref `h)})}
                               (di/add-side-dependency `side-dep)
                               (di/log :after-build! after-build!))]
      (t/is (= [`a `b `c `d `e `f `g `h
                ::root `di/new-key#1 `side-dep ::di/implicit-root] @log))
      (t/is (= {:a :a
                :b :b
                :c :c
                :d :d
                :e :e
                :f :f
                :g :g
                :h :h} @root)))))

(defn- side-dep2
  {::di/kind :component}
  []
  :side-dep2)

(t/deftest bug-array-map->hash-map-2
  (let [log          (atom [])
        after-build! (fn [{:keys [key]}]
                       (swap! log conj key))]
    (with-open [root (di/start ::root
                               (di/add-side-dependency `side-dep)
                               {::root (di/template
                                        {:a (di/ref `a)
                                         :b (di/ref `b)
                                         :c (di/ref `c)
                                         :d (di/ref `d)
                                         :e (di/ref `e)
                                         :f (di/ref `f)
                                         :g (di/ref `g)
                                         :h (di/ref `h)})}
                               (di/add-side-dependency `side-dep2)
                               (di/log :after-build! after-build!))]
      (t/is (= [`a `b `c `d `e `f `g `h
                ::root
                `di/new-key#0 `side-dep
                `di/new-key#2 `side-dep2
                ::di/implicit-root]
               @log))
      (t/is (= {:a :a
                :b :b
                :c :c
                :d :d
                :e :e
                :f :f
                :g :g
                :h :h} @root)))))

(t/deftest bug-with-update-key
  #_
  (let [info (di/inspect ::root
                         {::root     42
                          ::unused   0
                          ::side-dep :side-dep}
                         (di/add-side-dependency ::side-dep)
                         (di/update-key ::unused inc))
        keys (into #{}
                   (map :key)
                   info)]
    (t/is (contains? keys ::side-dep))))
