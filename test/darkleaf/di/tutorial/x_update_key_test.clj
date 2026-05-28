;; # Update key

(ns darkleaf.di.tutorial.x-update-key-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; `di/update-key` rewires an existing key: the original value is
;; built first, your function transforms it, and the result is what
;; every dependent sees. Multiple `update-key` calls on the same
;; target apply in registration order — each transforms the result of
;; the previous.

;; This is the main tool for composing across namespaces — the
;; namespace that owns a key doesn't need to know about the modules
;; that decorate or extend it. It covers the two cases Integrant has
;; no clean answer for: AOP-style wrappers around components and
;; shared registries assembled from independent modules (see
;; [Integrant vs DI](/doc/integrant.md)).

;; ## Decorate the built value

;; `(di/update-key target f & args)` applies `f` to the built value
;; of `target`, threading it as the first argument. The classic case
;; is the decorator pattern: wrap the original in something with the
;; same shape that delegates to it, adding behaviour. In Clojure this
;; is usually a higher-order `wrap-X` — takes the thing, returns a
;; wrapped thing of the same kind.

(defn handler [-deps req]
  {:status 200 :body (:uri req)})

(defn wrap-log [handler *log]
  (fn [req]
    (swap! *log conj (:uri req))
    (handler req)))

(t/deftest decorate-test
  (let [*log (atom [])]
    (with-open [root (di/start `handler
                               (di/update-key `handler wrap-log *log))]
      (t/is (= {:status 200 :body "/a"} (root {:uri "/a"})))
      (t/is (= {:status 200 :body "/b"} (root {:uri "/b"})))
      (t/is (= ["/a" "/b"] @*log)))))

;; ## Extend a collection

;; Any argument after `f` is itself a factory and gets built. This
;; lets each module attach itself to a shared registry: it owns its
;; handler and the route entry that wires the handler in, then hooks
;; the entry onto `routes` with `di/ref`. The namespace that defines
;; `routes` never references any of the modules.

(defn user-handler [-deps -req]
  :user)

(def user-route (di/template ["/users" (di/ref `user-handler)]))


(defn order-handler [-deps -req]
  :order)

(def order-route (di/template ["/orders" (di/ref `order-handler)]))


(def routes [])

(t/deftest extend-test
  (with-open [root (di/start `routes
                             (di/update-key `routes conj (di/ref `user-route))
                             (di/update-key `routes conj (di/ref `order-route)))]
    (t/is (= [["/users"  :user]
              ["/orders" :order]]
             (for [[path handler] @root]
               [path (handler :req)])))))
