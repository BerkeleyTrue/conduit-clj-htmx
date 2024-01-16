(ns conduit.infra.datalevin
  (:require
   [integrant.core :as ig]
   [datalevin.core :as d]))

(defmethod ig/init-key :infra.db/datalevin-kv [_ {:keys [config]}]
  (d/open-kv config))

(defmethod ig/halt-key! :infra.db/datalevin-kv [_ conn]
  (d/close-kv conn))
