(ns conduit.app.drivers.core
  (:require
    [integrant.core :as ig]
    [conduit.app.drivers.home :as home]))

(defmethod ig/init-key :app.routes/drivers
  [_ _]
  ["/"
   ["" {:name :get-home
        :get home/get-home-page}]])

(derive :app.routes/drivers :app/routes)
