(ns conduit.infra.http
  (:require
   [integrant.core :as ig]
   [aleph.http :as http]
   [taoensso.timbre :as timbre]))

(defmethod ig/init-key :infra/http [_ {:keys [handler port]}]
  (timbre/info "Starting HTTP server on port" port)
  (http/start-server handler {:port port}))

(defmethod ig/halt-key! :infra/http [_ ^java.lang.AutoCloseable server]
  (timbre/info "Stopping HTTP server")
  (.close server))
