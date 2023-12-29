(ns conduit.infra.http
  (:require
   [clojure.core.async :refer [put!]]
   [integrant.core :as ig]
   [aleph.http :as http]
   [taoensso.timbre :as timbre]))

(defmethod ig/init-key :infra/http [_ {:keys [handler port on-start-ch shutdown-timeout]}]
  (timbre/info "Starting HTTP server on port" port)
  (let [s (http/start-server handler {:port port
                                      :shutdown-timeout shutdown-timeout})]
    (timbre/info "HTTP server started")
    (put! on-start-ch "started")
    s))

(defmethod ig/halt-key! :infra/http [_ ^java.lang.AutoCloseable server]
  (timbre/info "Stopping HTTP server")
  (.close server))
