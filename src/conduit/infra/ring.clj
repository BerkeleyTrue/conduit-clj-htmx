(ns conduit.infra.ring
  (:require
    [integrant.core :as ig]
    [reitit.ring :as ring]))

(derive :reitit.routes/health :reitit/routes)

(defmethod ig/init-key :reitit.routes/health
  [_ _]
  ["/ping" {:name ::health
            :get (fn [_] {:status 200
                          :body "pong"})}])

(defmethod ig/init-key :router/core
  [_ {:keys [routes] :as opts}]
  (taoensso.timbre/log :info "Initializing router" routes)
  (ring/router ["" opts routes]))

(defmethod ig/init-key :router/routes
  [_ {:keys [routes]}]
  (taoensso.timbre/log :info "router" routes)
  (apply conj [] routes))
