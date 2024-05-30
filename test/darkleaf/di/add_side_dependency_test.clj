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

(defn- a [{log ::log}]
  (swap! log conj :a)
  :a)

(defn- b [{log ::log}]
  (swap! log conj :b)
  :b)

(defn- c [{log ::log}]
  (swap! log conj :c)
  :c)

(defn- d [{log ::log}]
  (swap! log conj :d)
  :d)

(defn- e [{log ::log}]
  (swap! log conj :e)
  :e)

(defn- f [{log ::log}]
  (swap! log conj :f)
  :f)

(defn- g [{log ::log}]
  (swap! log conj :g)
  :g)

(defn- h [{log ::log}]
  (swap! log conj :h)
  :h)

(defn- side-dep [{log ::log}]
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
      (t/is (= [:a :b :c :d :e :f :g :h :side-dep] @log))
      (t/is (= {:a :a
                :b :b
                :c :c
                :d :d
                :e :e
                :f :f
                :g :g
                :h :h} @root)))))
