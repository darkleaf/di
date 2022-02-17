(ns io.github.darkleaf.di.impl.map-destructuring-parser)

(defn- or-fn [a b]
  (or a b))

(def merge-deps (partial merge-with or-fn))

(defn- parse-keys [k v defaults]
  (when (and (keyword? k)
             (= "keys" (name k))
             (vector? v))
    (reduce
     (fn [deps binding]
       (let [ident     (keyword (namespace k) (name binding))
             required? (not (contains? defaults binding))]
         (assoc deps ident required?)))
     {}
     v)))

(defn- parse-syms [k v defaults]
  (when (and (keyword? k)
             (= "syms" (name k))
             (vector? v))
    (reduce
     (fn [deps binding]
       (let [ident     (symbol (namespace k) (name binding))
             required? (not (contains? defaults binding))]
         (assoc deps ident required?)))
     {}
     v)))

(defn- parse-strs [k v defaults]
  (when (and (= :strs k)
             (vector? v))
    (reduce
     (fn [deps binding]
       (let [ident     (name binding)
             required? (not (contains? defaults binding))]
         (assoc deps ident required?)))
     {}
     v)))

(defn- parse-named-key [k v defaults]
  (when (and (simple-symbol? k)
             (keyword? v))
    (let [ident     v
          required? (not (contains? defaults k))]
      {ident required?})))

(defn- parse-named-sym [k v defaults]
  (when (and (simple-symbol? k)
             (seq? v)
             (= 'quote (first v))
             (symbol? (second v)))
    (let [ident     (second v)
          required? (not (contains? defaults k))]
      {ident required?})))

(defn- parse-named-str [k v defaults]
  (when (and (simple-symbol? k)
             (string? v))
    (let [ident     v
          required? (not (contains? defaults k))]
      {ident required?})))

(defn parse
  "Parses destructuring map into map of ident and `required?` flag"
  [m]
  (let [defaults (:or m)
        m        (dissoc m :or :as)]
    (reduce-kv (fn [deps k v]
                 (merge deps
                        (parse-keys k v defaults)
                        (parse-syms k v defaults)
                        (parse-strs k v defaults)
                        (parse-named-key k v defaults)
                        (parse-named-sym k v defaults)
                        (parse-named-str k v defaults)))
               {}
               m)))
