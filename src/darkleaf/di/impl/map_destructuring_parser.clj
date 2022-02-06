(ns darkleaf.di.impl.map-destructuring-parser)

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

(defn- parse-named-keys [k v defaults]
  (when (and (simple-symbol? k)
             (keyword? v))
    {v (get defaults k)}))

(defn- parse-named-syms [k v defaults]
  (when (and (simple-symbol? k)
             (seq? v)
             (= 'quote (first v))
             (symbol? (second v)))
    {(second v) (get defaults k)}))

(defn parse [m]
  (let [defaults (:or m)
        m        (dissoc m :or :as)]
    (reduce-kv (fn [acc k v]
                 (merge acc
                        (parse-keys k v defaults)
                        (parse-syms k v defaults)
                        (parse-named-keys k v defaults)
                        (parse-named-syms k v defaults)))
               {}
               m)))
