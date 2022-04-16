(ns darkleaf.di.destructuring-map
  (:refer-clojure :exclude [key]))

(defn- key-tag [defaults key]
  (if (contains? defaults key)
      :optional
      :required))

(defn- parse-keys [k v defaults]
  (when (and (keyword? k)
             (= "keys" (name k))
             (vector? v))
    (reduce
     (fn [deps binding]
       (let [key (keyword (namespace k) (name binding))
             tag (key-tag defaults binding)]
         (assoc deps key tag)))
     {}
     v)))

(defn- parse-syms [k v defaults]
  (when (and (keyword? k)
             (= "syms" (name k))
             (vector? v))
    (reduce
     (fn [deps binding]
       (let [key       (symbol (namespace k) (name binding))
             tag (key-tag defaults binding)]
         (assoc deps key tag)))
     {}
     v)))

(defn- parse-strs [k v defaults]
  (when (and (= :strs k)
             (vector? v))
    (reduce
     (fn [deps binding]
       (let [key (name binding)
             tag (key-tag defaults binding)]
         (assoc deps key tag)))
     {}
     v)))

(defn- parse-named-key [k v defaults]
  (when (and (simple-symbol? k)
             (keyword? v))
    (let [key v
          tag (key-tag defaults k)]
      {key tag})))

(defn- parse-named-sym [k v defaults]
  (when (and (simple-symbol? k)
             (seq? v)
             (= 'quote (first v))
             (symbol? (second v)))
    (let [key (second v)
          tag (key-tag defaults k)]
      {key tag})))

(defn- parse-named-str [k v defaults]
  (when (and (simple-symbol? k)
             (string? v))
    (let [key v
          tag (key-tag defaults k)]
      {key tag})))

(defn dependencies
  "Parses destructuring map into map of dependency key and its type"
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
