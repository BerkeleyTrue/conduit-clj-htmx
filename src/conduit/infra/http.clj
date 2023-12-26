(ns conduit.infra.http
  (:require
   [integrant.core :as ig]
   [ring.adapter.jetty :as jetty])
  (:import
   [org.eclipse.jetty.server Server]))

; TODO: replace with aleph.http
(defmethod ig/init-key :infra/http [_ {:keys [handler port]}]
  (jetty/run-jetty handler {:port port :join? false :async? true}))

(defmethod ig/halt-key! :infra/http [_ ^Server server]
  (.stop server))
