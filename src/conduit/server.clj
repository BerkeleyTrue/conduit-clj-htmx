(ns conduit.server
  (:require
   [integrant.core :as ig]
   [reitit.ring  :as ring]
   [ring.util.http-response :as http-response]
   [conduit.infra.jetty]
   [conduit.infra.ring]))

(defmethod ig/init-key :handler/ring [_ {:keys [router]}]
  (ring/ring-handler
    router
    (ring/routes
      (ring/redirect-trailing-slash-handler)
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler
        {:not-found
         (constantly
           (->
             {:status 404
              :body "Resource not found"}
             (http-response/content-type "text/html")))
         :method-not-allowed
         (constantly
           (->
             {:status 405
              :body "Not Allowed"}
             (http-response/content-type "text/html")))
         :not-acceptable
         (constantly
           (->
             {:status 406
              :body "Not Acceptable"}
             (http-response/content-type "text/html")))}))))
