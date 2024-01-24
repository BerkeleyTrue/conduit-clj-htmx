(ns conduit.app.drivers.core
  (:require
    [integrant.core :as ig]
    [conduit.app.drivers.home :as home]
    [conduit.app.drivers.auth :as auth]
    [conduit.app.drivers.layout :refer [render-middleware]]
    [conduit.app.drivers.hot-reload :as hot-reload]))

(defmethod ig/init-key :app.routes/drivers
  [_ {:keys [on-start-ch user-service]}]
  ["/"
   {:middleware [render-middleware]}
   ["" {:name :get-home
        :get home/get-home-page}]
   ["__hotreload" {:name :hotreload
                   :get (hot-reload/->get-sse on-start-ch)}]
   (auth/->login-routes user-service)
   (auth/->register-routes user-service)])

(derive :app.routes/drivers :app/routes)
