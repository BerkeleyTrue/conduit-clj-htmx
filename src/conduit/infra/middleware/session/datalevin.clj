(ns conduit.infra.middleware.session.datalevin
  (:require
   [datalevin.core :as d]
   [ring.middleware.session.store :refer [SessionStore]]
   [integrant.core :as ig]))

(def table "session-store")

(deftype DatalevinStore [db]
  SessionStore
  (read-session [_ key]
    (d/get-value db table key))
  (write-session [_ key val]
    (d/transact-kv
      db
      [[:put table key val]]))
  (delete-session [_ key]
    (d/transact-kv
      db
      [[:del table key]])))


(defn datalevin-store [db]
  (DatalevinStore. db))

(defmethod ig/init-key :infra.middleware.session/datalevin [_ {:keys [db]}]
  (datalevin-store db))
