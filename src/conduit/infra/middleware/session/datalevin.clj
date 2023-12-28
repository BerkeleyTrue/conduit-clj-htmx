(ns conduit.infra.middleware.session.datalevin
  (:require
   [ring.middleware.session.store :refer [SessionStore]]
   [integrant.core :as ig]
   [taoensso.timbre :as timbre]
   [conduit.infra.datalevin :as d]))

(def dbi "session-store")

(deftype DatalevinStore [db]
  SessionStore
  (read-session [_ key]
    (d/get-value-kv db dbi key))
  (write-session [_ key val]
    (d/put-value-kv db dbi key val))
  (delete-session [_ key]
    (d/del-value-kv db dbi key)))

(defn datalevin-store [db]
  (DatalevinStore. db))

(defmethod ig/init-key :infra.middleware.session/datalevin [_ {:keys [db]}]
  (timbre/info "Initializing Datalevin session store" db)
  (datalevin-store db))
