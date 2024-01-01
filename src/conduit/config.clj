(ns conduit.config
  (:require
   [clojure.java.io :as io]
   [aero.core :as aero]
   [integrant.core :as ig]
   [conduit.env :as env]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defmethod aero/reader 'ig/refset [_ _ value]
  (ig/refset value))

(defn get-config []
  (aero/read-config (io/resource "system.edn") (:config env/defaults)))

(def config (get-config))
