(ns darkleaf.di.internal.cljdoc
  {:no-doc true}
  (:require
   [cljdoc.doc-tree :as doc-tree]
   [cljdoc.render.rich-text :as rich-text]))

(defn clj->md [content]
  (str "```clojure\n" content "\n```"))

(defmethod doc-tree/filepath->type "clj" [_]
  :darkleaf/clj)

(defmethod rich-text/render-text :darkleaf/clj [[_ content]]
  (-> content
      clj->md
      rich-text/markdown-to-html))

(defmethod rich-text/determine-features :darkleaf/clj [[_ content]]
  nil)
