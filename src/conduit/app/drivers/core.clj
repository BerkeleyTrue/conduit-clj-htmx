(ns conduit.app.drivers.core
  (:require
    [integrant.core :as ig]
    [conduit.app.drivers.home :as home]
    [conduit.app.drivers.auth :as auth]))

(defmethod ig/init-key :app.routes/drivers
  [_ _]
  ["/"
   ["" {:name :get-home
        :get home/get-home-page}]
   ["login"
    {:name :login
     :get auth/get-login-page}]
   ["register"
    {:name :register
     :get auth/get-register-page}]])

(derive :app.routes/drivers :app/routes)
