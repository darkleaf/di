(ns io.github.darkleaf.di.impl.map-destructuring-parser)

(defn- parse-keys [k v]
  (when (and (keyword? k)
             (= "keys" (name k))
             (vector? v))
    (reduce
     (fn [deps binding]
       (conj deps (keyword (namespace k)
                           (name binding))))
     #{}
     v)))

(defn- parse-syms [k v]
  (when (and (keyword? k)
             (= "syms" (name k))
             (vector? v))
    (reduce
     (fn [deps binding]
       (conj deps (symbol (namespace k)
                          (name binding))))
     #{}
     v)))

(defn- parse-strs [k v]
  (when (and (= :strs k)
             (vector? v))
    (reduce
     (fn [deps binding]
       (conj deps (name binding)))
     #{}
     v)))

(defn- parse-named-key [k v]
  (when (and (simple-symbol? k)
             (keyword? v))
    #{v}))

(defn- parse-named-sym [k v]
  (when (and (simple-symbol? k)
             (seq? v)
             (= 'quote (first v))
             (symbol? (second v)))
    #{(second v)}))

(defn- parse-named-str [k v]
  (when (and (simple-symbol? k)
             (string? v))
    #{v}))

(defn parse [m]
  (let [m (dissoc m :or :as)]
    (reduce-kv (fn [deps k v]
                 (-> deps
                     (into (parse-keys k v))
                     (into (parse-syms k v))
                     (into (parse-strs k v))
                     (into (parse-named-key k v))
                     (into (parse-named-sym k v))
                     (into (parse-named-str k v))))
               #{}
               m)))
