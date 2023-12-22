(ns conduit.infra.datalevin
  (:require
    [integrant.core :as ig]
    [datalevin.core :as d]))


(defmethod ig/init-key :infra.db/datalevin [_ {:keys [config]}]
  (d/get-conn config))

(defmethod ig/halt-key! :infra.db/datalevin [_ conn]
  (d/close conn))
