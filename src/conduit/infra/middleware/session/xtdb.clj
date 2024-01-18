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
      (first
       (xt/q
        (xt/db node)
        '{:find [?session-id ?session-data]
          :where [[?session-id :session/id key]
                  [?session-id :session/data ?session-data]]
          :in [key]}
        key))))

  (write-session [_ key val]
    (let [key (or key (str key (UUID/randomUUID)))]
      (xt/submit-tx
        node
        [[::xt/put
          {:xt/id key
           :session/id key
           :session/data val}]])
      key))

  (delete-session [_ key]
    (xt/submit-tx
      node
      [[::xt/delete key]])))

(defmethod ig/init-key :infra.middleware.session/xtdb [_ {:keys [node]}]
  (timbre/info "Initializing xtdb session store")
  (->XtdbStore node))
