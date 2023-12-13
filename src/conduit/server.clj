(ns conduit.server
  (:require
   [integrant.core :as ig]
   [ring.adapter.jetty :as jetty])
  (:import
   [org.eclipse.jetty.server Server]))

(defmethod ig/init-key :handler/greet [_ _]
  (fn [_]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body "Hello, World!"}))

(defmethod ig/init-key :adapter/jetty [_ {:keys [handler port]}]
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! :adapter/jetty [_ ^Server server]
  (.stop server))
