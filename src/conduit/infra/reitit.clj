(ns conduit.infra.reitit
  (:require
   [taoensso.timbre :as timbre]
   [integrant.core :as ig]
   [muuntaja.core :as m]
   [muuntaja.format.form :as muu.form]
   [ring.util.response :as response]
   [ring.middleware.defaults :refer [site-defaults]]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muu.mid]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.defaults :refer [ring-defaults-middleware]]
   [reitit.coercion.malli :as coercion.malli]
   [conduit.infra.middleware.coercion :refer [coerce-exceptions-htmx-middleware]]
   [conduit.env :as env]))

(defmethod ig/init-key :infra.router/core
  [_ {:keys [routes session-store] :as opts}]
  (let [routes (conj routes ["/public" (ring/create-resource-handler)])]
    (timbre/info "init router " routes)
    (ring/router
     ["" opts routes]
     {:data
      {:coercion (coercion.malli/create)

       :middleware (into
                    (:middleware env/defaults)
                    (into
                      [exception/exception-middleware] ; TODO: don't need both this ans wrap-stacktrace, need to come up with a better way to handle exceptions
                      (conj
                        ring-defaults-middleware
                        muu.mid/format-middleware
                        coerce-exceptions-htmx-middleware
                        coercion/coerce-request-middleware
                        coercion/coerce-response-middleware)))

       :defaults (->
                  site-defaults
                  (assoc :exception true)
                  (assoc-in [:parameters :urlencoded] false)
                  (assoc-in [:session :store] session-store))

       :muuntaja (m/create (-> m/default-options
                               (assoc-in [:formats "application/x-www-form-urlencoded"] muu.form/format)))}})))

(defmethod ig/init-key :infra.ring/handler [_ {:keys [router]}]
  (let [default-handler (ring/routes ; default handler
                         (ring/redirect-trailing-slash-handler)
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
