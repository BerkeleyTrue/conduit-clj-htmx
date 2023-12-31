(ns conduit.infra.datalevin
  (:require
   [integrant.core :as ig]
   [datalevin.core :as d]
   [conduit.app.driving.user-repo :refer [user-schema]]))

(defmethod ig/init-key :infra.db/datalevin [_ {:keys [config]}]
  (d/get-conn config user-schema))

(defmethod ig/halt-key! :infra.db/datalevin [_ conn]
  (d/close conn))

(defmethod ig/init-key :infra.db/datalevin-kv [_ {:keys [config]}]
  (d/open-kv config))

(defmethod ig/halt-key! :infra.db/datalevin-kv [_ conn]
  (d/close-kv conn))
