(ns conduit.infra.reitit
  (:require
   [taoensso.timbre :as timbre]
   [integrant.core :as ig]
   [muuntaja.format.form :as muu.form]
   [ring.util.response :as response]
   [ring.middleware.defaults :refer [site-defaults]]
   [reitit.coercion.malli :as coercion.malli]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.defaults :refer [ring-defaults-middleware]]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.ring.middleware.muuntaja :as muu.reitit]
   [reitit.ring.middleware.exception :as exception]
   [conduit.env :as env]))

(defmethod ig/init-key :infra.router/core
  [_ {:keys [routes] :as opts}]
  (timbre/info "init router " opts)
  (ring/router
   ["" opts routes]
   {:data {:coercion
           (coercion.malli/create
            {:transformers
             {:body {:default reitit.coercion.malli/default-transformer-provider
                     :formats {"application/x-www-form-urlencoded" muu.form/format}}}})
           :middleware
           [parameters/parameters-middleware
            muu.reitit/format-negotiate-middleware
            muu.reitit/format-request-middleware
            exception/exception-middleware
            muu.reitit/format-response-middleware
            coercion/coerce-request-middleware
            coercion/coerce-response-middleware
            ring-defaults-middleware
            (:middleware env/defaults)]
           :defaults (-> site-defaults)}}))

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
