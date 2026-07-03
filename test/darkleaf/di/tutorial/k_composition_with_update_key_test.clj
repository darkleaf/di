;; # Composition with `update-key`

(ns darkleaf.di.tutorial.k-composition-with-update-key-test
  (:require
   [clojure.test :as t]
   [darkleaf.di.core :as di]))

;; `di/update-key` rewires an existing key: the original value is
;; built first, your function transforms it, and the result is
;; what every dependent sees. Multiple `update-key` calls on the
;; same target apply in registration order — each transforms the
;; result of the previous.

;; This is the main tool for cross-namespace composition: the
;; namespace that owns a key does not need to know about the
;; modules that decorate or extend it. Two patterns below cover
;; most uses — wrapping a value with extra behaviour, and
;; extending a shared collection.

;; ## Decorate the built value

;; `(di/update-key target f & args)` applies `f` to the built
;; value of `target`, threading it as the first argument. The
;; classic case is the decorator pattern: wrap the original in
;; something with the same shape that delegates to it, adding
;; behaviour. In Clojure this is usually a higher-order `wrap-X`
;; — takes the thing, returns a wrapped thing of the same kind.

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
;; lets each module attach itself to a shared registry: it owns
;; its handler and the route entry that wires the handler in,
;; then hooks the entry onto `routes` with `di/ref`. The namespace
;; that defines `routes` never references any of the modules.

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

;; Everything `update-key` takes is a factory — the function
;; included. Plain values like `wrap-log`, `conj`, or `*log` above
;; are constants and pass through as is, but any position accepts
;; any factory — `di/ref` or `di/template`, for example. With
;; ``(di/update-key `handler (di/ref `wrap-metrics))`` the
;; decorator itself is built by the system and receives
;; dependencies of its own — say, a stateful metrics registry.
;; And an argument can be assembled too: pass plain `wrap-cache`
;; with ``(di/template {:store (di/ref `redis)})``, and the
;; wrapper receives an options map with the started store inside.

;; (Under the hood `di/update-key` is a middleware — see
;; [The middleware argument](/doc/reference/middleware_argument.md)
;; for what that means. For everyday use you just need to know
;; what update-key does.)

;; The next chapter shows what DI does when a component throws
;; halfway through start.
