(ns conduit.infra.http
  (:require
   [integrant.core :as ig]
   [ring.adapter.jetty :as jetty])
  (:import
   [org.eclipse.jetty.server Server]))

(defmethod ig/init-key :infra/http [_ {:keys [handler port]}]
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! :infra/http [_ ^Server server]
  (.stop server))
