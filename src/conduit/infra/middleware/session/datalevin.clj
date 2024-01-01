(ns conduit.infra.middleware.session.datalevin
  (:require
   [ring.middleware.session.store :refer [SessionStore]]
   [integrant.core :as ig]
   [taoensso.timbre :as timbre]
   [datalevin.core :as d])
  (:import [java.util UUID]))

(def dbi "session-store")

(deftype DatalevinStore [db]
  SessionStore
  (read-session [_ key]
    (when (not (nil? key))
      (d/get-value db dbi key)))
  (write-session [_ key val]
    (let [key (or key (str key (UUID/randomUUID)))]
      (d/transact-kv db [[:put dbi key val]])
      key))
  (delete-session [_ key]
    (d/transact-kv db [[:del dbi key]])))

(defn datalevin-store [db]
  (DatalevinStore. db))

(defmethod ig/init-key :infra.middleware.session/datalevin [_ {:keys [db]}]
  (timbre/info "Initializing atalevin session store")
  (d/open-dbi db dbi)
  (datalevin-store db))
