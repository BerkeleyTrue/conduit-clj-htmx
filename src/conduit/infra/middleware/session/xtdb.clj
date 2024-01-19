(ns conduit.infra.middleware.session.xtdb
  (:require
   [ring.middleware.session.store :refer [SessionStore]]
   [integrant.core :as ig]
   [taoensso.timbre :as timbre]
   [xtdb.api :as xt])
  (:import [java.util UUID]))

(deftype XtdbStore [node]
  SessionStore
  (read-session [_ key]
    (when (not (nil? key))
      (->
        node
        (xt/db)
        (xt/entity key)
        (:session/data))))

  (write-session [_ key val]
    (let [key (or key (str key (UUID/randomUUID)))
          tx-res (xt/submit-tx
                   node
                   [[::xt/put
                     {:xt/id key
                      :session/id key
                      :session/data val}]])]
      (xt/await-tx node tx-res)
      key))

  (delete-session [_ key]
    (xt/await-tx node (xt/submit-tx node [[::xt/delete key]]))
    nil))

(defmethod ig/init-key :infra.middleware.session/xtdb [_ {:keys [node]}]
  (timbre/info "Initializing xtdb session store")
  (->XtdbStore node))
