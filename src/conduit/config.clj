(ns conduit.config
  (:require
   [aero.core :as aero]
   [integrant.core :as ig]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defmethod aero/reader 'ig/refset [_ _ value]
  (ig/refset value))

(defn get-config []
  (aero/read-config "system.edn"))

(def config (get-config))
