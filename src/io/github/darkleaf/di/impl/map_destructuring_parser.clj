(ns io.github.darkleaf.di.impl.map-destructuring-parser)

(defn- parse-keys [k v defaults]
  (when (and (keyword? k)
             (= "keys" (name k))
             (vector? v))
    (reduce
     (fn [acc binding]
       (let [ident   (keyword (namespace k)
                              (name binding))
             default (get defaults binding)]
         (assoc acc ident default)))
     {}
     v)))

(defn- parse-syms [k v defaults]
  (when (and (keyword? k)
             (= "syms" (name k))
             (vector? v))
    (reduce
     (fn [acc binding]
       (let [ident   (symbol (namespace k)
                             (name binding))
             default (get defaults binding)]
         (assoc acc ident default)))
     {}
     v)))

(defn- parse-strs [k v defaults]
  (when (and (= :strs k)
             (vector? v))
    (reduce
     (fn [acc binding]
       (let [ident   (name binding)
             default (get defaults binding)]
         (assoc acc ident default)))
     {}
     v)))

(defn- parse-named-key [k v defaults]
  (when (and (simple-symbol? k)
             (keyword? v))
    {v (get defaults k)}))

(defn- parse-named-sym [k v defaults]
  (when (and (simple-symbol? k)
             (seq? v)
             (= 'quote (first v))
             (symbol? (second v)))
    {(second v) (get defaults k)}))

(defn- parse-named-str [k v defaults]
  (when (and (simple-symbol? k)
             (string? v))
    {v (get defaults k)}))

(defn parse [m]
  (let [defaults (:or m)
        m        (dissoc m :or :as)]
    (reduce-kv (fn [acc k v]
                 (merge acc
                        (parse-keys k v defaults)
                        (parse-syms k v defaults)
                        (parse-strs k v defaults)
                        (parse-named-key k v defaults)
                        (parse-named-sym k v defaults)
                        (parse-named-str k v defaults)))
               {}
               m)))
