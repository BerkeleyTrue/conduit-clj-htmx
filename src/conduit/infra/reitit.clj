(ns conduit.infra.reitit
  (:require
   [taoensso.timbre :as timbre]
   [integrant.core :as ig]
   [muuntaja.format.form :as muu.form]
   [ring.util.response :as response]
   [ring.middleware.defaults :refer [site-defaults]]
   [ring.middleware.session.cookie :refer [cookie-store]]
   [reitit.coercion.malli :as coercion.malli]
   [reitit.ring :as ring]
   [reitit.ring.middleware.defaults :refer [defaults-middleware]]
   [conduit.env :as env]))

; TODO: replace with datalevin store
(def default-session-storage
  (cookie-store))

(defmethod ig/init-key :infra.router/core
  [_ {:keys [routes] :as opts}]
  (timbre/info "init router " opts)
  (ring/router
   ["" opts routes]
   {:data
    {:coercion
      (coercion.malli/create
       {:transformers
        {:body {:default coercion.malli/default-transformer-provider
                :formats {"application/x-www-form-urlencoded" muu.form/format}}}})
     :middleware (conj
                   defaults-middleware
                   (:middleware env/defaults))

     :defaults (->
                 site-defaults
                 (assoc :params false)
                 (assoc-in [:session :store] default-session-storage))}}))

(defmethod ig/init-key :infra.ring/handler [_ {:keys [router]}]
  (let [default-handler (ring/routes ; default handler
                         (ring/redirect-trailing-slash-handler)
                         (ring/create-resource-handler {:path "/"})
                         (ring/create-default-handler
                          {:not-found
                           (constantly
                            (->
                             {:status 404
                              :body "Resource not found"}
                             (response/content-type "text/html")))
                           :method-not-allowed
                           (constantly
                            (->
                             {:status 405
                              :body "Not Allowed"}
                             (response/content-type "text/html")))
                           :not-acceptable
                           (constantly
                            (->
                             {:status 406
                              :body "Not Acceptable"}
                             (response/content-type "text/html")))}))]
    (ring/ring-handler router default-handler)))
