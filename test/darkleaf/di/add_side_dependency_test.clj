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

(defn- d1
  {::di/kind :component}
  []
  :d1)

(defn- d2
  {::di/kind :component}
  []
  :d2)

(defn- d3
  {::di/kind :component}
  []
  :c)

(defn- d4
  {::di/kind :component}
  []
  :d4)

(defn- d5
  {::di/kind :component}
  []
  :d5)

(defn- d6
  {::di/kind :component}
  []
  :d6)

(defn- d7
  {::di/kind :component}
  []
  :d7)

(defn- d8
  {::di/kind :component}
  []
  :d8)

(defn- extra-d9
  {::di/kind :component}
  []
  :d9)

(defn- extra-d10
  {::di/kind :component}
  []
  :d10)


(t/deftest bug-array-map->hash-map
  (let [log          (atom [])
        after-build! (fn [{:keys [key]}]
                       (swap! log conj key))]
    (with-open [root (di/start ::root
                               {::root :ok}
                               (di/add-side-dependency `d1)
                               (di/add-side-dependency `d2)
                               (di/add-side-dependency `d3)
                               (di/add-side-dependency `d4)
                               (di/add-side-dependency `d5)
                               (di/add-side-dependency `d6)
                               (di/add-side-dependency `d7)
                               (di/add-side-dependency `d8)
                               (di/add-side-dependency `extra-d9)
                               (di/log :after-build! after-build!))]
      (t/is (= [::root `d1 `d2 `d3 `d4 `d5 `d6 `d7 `d8 `extra-d9 ::di/implicit-root]
               @log))
      (t/is (= :ok @root)))))


(t/deftest bug-array-map->hash-map-2
  (let [log          (atom [])
        after-build! (fn [{:keys [key]}]
                       (swap! log conj key))]
    (with-open [root (di/start ::root
                               {::root :ok}
                               (di/add-side-dependency `extra-d9)
                               (di/add-side-dependency `d1)
                               (di/add-side-dependency `d2)
                               (di/add-side-dependency `d3)
                               (di/add-side-dependency `d4)
                               (di/add-side-dependency `d5)
                               (di/add-side-dependency `d6)
                               (di/add-side-dependency `d7)
                               (di/add-side-dependency `d8)
                               (di/add-side-dependency `extra-d10)
                               (di/log :after-build! after-build!))]
      (t/is (= [::root
                `extra-d9 `d1 `d2 `d3 `d4 `d5 `d6 `d7 `d8 `extra-d10
                ::di/implicit-root]
               @log))
      (t/is (= :ok @root)))))

(t/deftest bug-with-update-key
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
