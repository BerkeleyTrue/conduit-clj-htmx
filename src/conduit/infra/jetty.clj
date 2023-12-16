(ns conduit.infra.jetty
  (:require
   [integrant.core :as ig]
   [ring.adapter.jetty :as jetty])
  (:import
   [org.eclipse.jetty.server Server]))

(defmethod ig/init-key :server/http [_ {:keys [handler port]}]
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! :server/http [_ ^Server server]
  (.stop server))
