(ns conduit.infra.datalevin
  (:require
    [integrant.core :as ig]
    [datalevin.core :as d]))

(defn get-value-kv [db dbi key]
  (try
    (d/get-value db dbi key)
    (catch Exception _
      ; (timbre/error ex)
      nil)))

(defn put-value-kv [db dbi key val]
  (try
    (d/transact-kv db [[:put dbi key val]])
    (catch Exception _
      ; (timbre/error ex)
      nil)))

(defn del-value-kv [db dbi key]
  (try
    (d/transact-kv db [[:del dbi key]])
    (catch Exception _
      ; (timbre/error ex)
      nil)))

(defmethod ig/init-key :infra.db/datalevin [_ {:keys [config]}]
  (d/get-conn config))

(defmethod ig/halt-key! :infra.db/datalevin [_ conn]
  (d/close conn))

(defmethod ig/init-key :infra.db/datalevin-kv [_ {:keys [config]}]
  (d/open-kv config))

(defmethod ig/halt-key! :infra.db/datalevin-kv [_ conn]
  (d/close-kv conn))
