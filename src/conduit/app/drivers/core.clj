(ns conduit.app.drivers.core
  (:require
    [clojure.core.async :refer [chan]]
    [integrant.core :as ig]
    [conduit.app.drivers.home :as home]
    [conduit.app.drivers.auth :as auth]
    [conduit.app.drivers.hot-reload :as hot-reload]))

(defmethod ig/init-key :app.routes/drivers
  [_ _]
  ["/"
   ["" {:name :get-home
        :get home/get-home-page}]
   ["__hotreload" {:name :hotreload
                   :get (hot-reload/->get-sse (chan 1))}]
   ["login"
    {:name :login
     :get auth/get-login-page
     :post auth/post-login-page}]
   ["register"
    {:name :register
     :get auth/get-register-page}]])

(derive :app.routes/drivers :app/routes)
