(ns conduit.infra.ring
  (:require
   [integrant.core :as ig]
   [ring.util.response :as response]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [conduit.env :as env]))

(defmethod ig/init-key :infra.routes/health
  [_ _]
  ["/ping" {:name ::health
            :get (fn [_]
                   (->
                     "pong"
                     (response/response)
                     (response/content-type "text/html")))}])

(derive :infra.routes/health :infra/routes)

(comment
  (isa? :infra.routes/health :infra/routes))

(defmethod ig/init-key :infra.router/routes
  [_ {:keys [infra-routes routes]}]
  (into [] (concat infra-routes routes)))

(defn ->middleware []
  (let [env-middleware (:middleware env/defaults)]
    [#(wrap-defaults % site-defaults)
     env-middleware]))
