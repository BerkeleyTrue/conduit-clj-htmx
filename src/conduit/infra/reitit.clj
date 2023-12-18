(ns conduit.infra.reitit
  (:require
    [integrant.core :as ig]
    [taoensso.timbre :refer [info]]
    [ring.util.response :as response]
    [reitit.ring :as ring]))

(defmethod ig/init-key :infra.router/core
  [_ {:keys [routes] :as opts}]
  (info "init router " opts)
  (ring/router ["" opts routes]))

(defmethod ig/init-key :infra.ring/handler [_ {:keys [router]}]
  (ring/ring-handler
    router
    (ring/routes
      (ring/redirect-trailing-slash-handler)
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler
        {:not-found
         (constantly
           (->
             {:status 403
              :body "Resource not found"}
             (response/content-type "text/html")))
         :method-not-allowed
         (constantly
           (->
             {:status 404
              :body "Not Allowed"}
             (response/content-type "text/html")))
         :not-acceptable
         (constantly
           (->
             {:status 405
              :body "Not Acceptable"}
             (response/content-type "text/html")))}))))
