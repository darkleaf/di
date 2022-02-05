(ns darkleaf.di.impl.deps-parser)

(defmacro ^:private <<- [& body]
 `(->> ~@(reverse body)))

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


(defn parse-destructuring-map [m]
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

(defn parse [v]
  (<<-
   (let [arglists (-> v meta :arglists)])
   (if (nil? arglists)
     (throw (ex-info "a" {})))
   (if (not= 1 (count arglists))
     (throw (ex-info "b" {})))
   (let [arglist (first arglists)])
   (if (empty? arglist)
     (throw (ex-info "c" {})))
   (let [deps (first arglist)])
   (if-not (map? deps)
     (throw (ex-info "d" {})))
   (parse-destructuring-map deps)))
