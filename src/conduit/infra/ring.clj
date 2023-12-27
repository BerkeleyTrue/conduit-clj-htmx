(ns conduit.infra.ring
  (:require
   [integrant.core :as ig]
   [muuntaja.middleware :as muu.middleware]
   [muuntaja.format.form :as muu.form]
   [muuntaja.core :as muu.core]
   [ring.util.response :as response]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [conduit.env :as env]))

(def m (muu.core/create
         (->
           muu.core/default-options
           (assoc-in [:formats "application/x-www-form-urlencoded"] muu.form/format))))

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

(defn ->middleware [handler]
  (->
    handler
    (muu.middleware/wrap-params)
    (wrap-defaults (-> site-defaults (assoc-in [:params :urlencoded] false)))
    (muu.middleware/wrap-format m)
    ((:middleware env/defaults))))
