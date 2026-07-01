;; # Visualizing your system

(ns darkleaf.di.how-to.visualizing-test
  (:require
   [clojure.string :as str]
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; `di/inspect` returns the dependency graph as plain Clojure
;; data: a vector of maps, one per key, each carrying a `:key`
;; and its `:dependencies`. Plain data is easy to turn into a
;; picture.
;; This recipe turns that data into the
;; [DOT language](https://graphviz.org/doc/info/lang.html) and
;; renders it with [Graphviz](https://graphviz.org/).

;; `di` ships no drawing code — the function below lives in your
;; project, so you can shape the output however you like.

;; A small system to draw: `a` depends on `b` and `c`, and `b`
;; depends on `c`.

(defn c
  {::di/kind :component}
  []
  :c)

(defn b [{c `c}]
  [:b c])

(defn a [{b `b, c `c}]
  [:a b c])

;; The `graphviz` function takes the output of `di/inspect` and
;; returns the graph as text in the DOT language. For every map
;; it prints one edge from its `:dependencies` to its `:key`. A
;; map with no `:dependencies` prints an empty set on the left,
;; which Graphviz reads as a standalone box.

(defn- graphviz-id [x]
  (str "\"" x "\""))

(defn graphviz [components]
  (with-out-str
    (println "digraph {")
    (println "rankdir=LR;")
    (println "node [shape=box color=gray style=rounded];")
    (println "edge [color=gray arrowhead=empty];")
    (doseq [{:keys [key dependencies]} components]
      (print "{")
      (doseq [token (interpose "," (map graphviz-id (keys dependencies)))]
        (print token))
      (println "}" "->" (graphviz-id key)))
    (println "}")))

;; For the system above it produces:

(def result
  (-> "digraph {
rankdir=LR;
node [shape=box color=gray style=rounded];
edge [color=gray arrowhead=empty];
{'darkleaf.di.how-to.visualizing-test/b','darkleaf.di.how-to.visualizing-test/c'} -> 'darkleaf.di.how-to.visualizing-test/a'
{'darkleaf.di.how-to.visualizing-test/c'} -> 'darkleaf.di.how-to.visualizing-test/b'
{} -> 'darkleaf.di.how-to.visualizing-test/c'
}
"
      (str/replace "'" "\"")))

(t/deftest graphviz-test
  ;; Each component becomes one edge: its dependencies point at
  ;; it. `c` has no dependencies, so its left-hand set is empty.
  (t/is (= result (graphviz (di/inspect `a)))))

;; ## Rendering

;; `graphviz` returns DOT text. Save it from a REPL and render it
;; with `dot`, the Graphviz engine for directed graphs:

;; ```clojure
;; (spit "system.dot" (graphviz (di/inspect `root)))
;; ```

;; ```
;; dot -Tsvg system.dot -o system.svg
;; ```

;; ![The rendered dependency graph](/doc/images/visualizing.svg)

;; The nodes are the fully qualified keys of your system, so the
;; picture matches the names you wrote.

;; This graph uses only `:key` and `:dependencies`. Each map from
;; `di/inspect` carries more — a `:description` with the key's
;; kind, the var behind it, and markers the walk adds. Reach for
;; those fields to style the picture: color by kind, highlight the
;; roots you asked for, or group keys by namespace with `subgraph`.
;; The [Inspect](/doc/reference/inspect_test.md) reference lists
;; every field.
