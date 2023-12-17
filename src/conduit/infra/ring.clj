(ns conduit.infra.ring
  (:require
   [integrant.core :as ig]
   [taoensso.timbre :refer [info]]
   [ring.util.response :as response]))

(defmethod ig/init-key :infra.routes/health
  [_ _]
  ["/ping" {:name ::health
            :get
            (fn [_]
              (->
                "pong"
                (response/response)
                (response/content-type "text/html")))}])

(derive :infra.routes/health :infra/routes)

(comment (isa? :infra.routes/health :infra/routes))

(defmethod ig/init-key :infra.router/routes
  [_ {:keys [infra-routes routes]}]
  (info "init routes " infra-routes)
  (apply conj [] infra-routes))
