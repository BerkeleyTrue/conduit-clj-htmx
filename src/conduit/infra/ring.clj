(ns conduit.infra.ring
  (:require
    [integrant.core :as ig]
    [reitit.ring :as ring]))

(derive :reitit.routes/health :reitit/routes)

(comment (isa? :reitit.routes/health :reitit/routes))

(defmethod ig/init-key :reitit.routes/health
  [_ _]
  ["/ping" {:name ::health
            :get (fn [_] {:status 200
                          :body "pong"})}])

(defmethod ig/init-key :router/routes
  [_ {:keys [routes]}]
  (apply conj [] routes))

(defmethod ig/init-key :router/core
  [_ {:keys [routes] :as opts}]
  (ring/router ["" opts routes]))
