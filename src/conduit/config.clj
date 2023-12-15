(ns conduit.config
  (:require
   [aero.core :as aero]
   [integrant.core :as ig]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))


(def config (aero/read-config "system.edn"))
