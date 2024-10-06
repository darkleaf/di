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
  [{log ::log}]
  (swap! log conj :a)
  :a)

(defn- b
  {::di/kind :component}
  [{log ::log}]
  (swap! log conj :b)
  :b)

(defn- c
  {::di/kind :component}
  [{log ::log}]
  (swap! log conj :c)
  :c)

(defn- d
  {::di/kind :component}
  [{log ::log}]
  (swap! log conj :d)
  :d)

(defn- e
  {::di/kind :component}
  [{log ::log}]
  (swap! log conj :e)
  :e)

(defn- f
  {::di/kind :component}
  [{log ::log}]
  (swap! log conj :f)
  :f)

(defn- g
  {::di/kind :component}
  [{log ::log}]
  (swap! log conj :g)
  :g)

(defn- h
  {::di/kind :component}
  [{log ::log}]
  (swap! log conj :h)
  :h)

(defn- side-dep
  {::di/kind :component}
  [{log ::log}]
  (swap! log conj :side-dep))

(t/deftest bug-array-map->hash-map
  (let [log (atom [])]
    (with-open [root (di/start ::root
                               {::log  log
                                ::root (di/template
                                        {:a (di/ref `a)
                                         :b (di/ref `b)
                                         :c (di/ref `c)
                                         :d (di/ref `d)
                                         :e (di/ref `e)
                                         :f (di/ref `f)
                                         :g (di/ref `g)
                                         :h (di/ref `h)})}
                               (di/add-side-dependency `side-dep))]
      (t/is (= [:side-dep :h :g :f :e :d :c :b :a] @log))
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
  [{log ::log}]
  (swap! log conj :side-dep2))

(t/deftest bug-array-map->hash-map-2
  (let [log (atom [])]
    (with-open [root (di/start ::root
                               {::log  log}
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
                               (di/add-side-dependency `side-dep2))]
      (t/is (= [:side-dep :side-dep2 :h :g :f :e :d :c :b :a] @log))
      (t/is (= {:a :a
                :b :b
                :c :c
                :d :d
                :e :e
                :f :f
                :g :g
                :h :h} @root)))))
