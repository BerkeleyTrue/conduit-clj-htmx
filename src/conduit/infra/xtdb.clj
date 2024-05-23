(ns conduit.infra.xtdb
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [taoensso.timbre :as timbre]
   [conduit.app.driving.user-repo :as user-repo]))

(def transaction-functions
  [[::xt/put
    {:xt/id :update-entity
     :xt/fn '(fn [ctx eid key f val]
               (let [db (xtdb.api/db ctx)
                     entity (xtdb.api/entity db eid)]
                 [[::xt/put (update entity key f val)]]))}]
   [::xt/put
    {:xt/id :assoc-entity
     :xt/fn '(fn [ctx eid key val]
               (let [db (xtdb.api/db ctx)
                     entity (xtdb.api/entity db eid)]
                 [[::xt/put (assoc entity key val)]]))}]])

(defmethod ig/init-key :infra.db/xtdb [_ {:keys [index-dir doc-dir log-dir]}]
  (let [node (xt/start-node
              {:xtdb/tx-log {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                                        :db-dir (io/file log-dir)}}
               :xtdb/document-store {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                                                :db-dir (io/file doc-dir)}}
               :xtdb/index-store {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                                             :db-dir (io/file index-dir)}}})
        _ (xt/submit-tx node (into [] (concat transaction-functions user-repo/transaction-functions)))
        f (future (xt/sync node))]

    (while (not (realized? f))
      (Thread/sleep 2000)
      (when-some [indexed (xt/latest-completed-tx node)]
        (timbre/info "Indexed: " (pr-str indexed))))

    node))

(defmethod ig/halt-key! :infra.db/xtdb [_ node]
  (.close node))

(defmethod ig/init-key :infra.db/xtdb-listener [_ {:keys [node]}]
  (xt/listen
   node
   {::xt/event-type ::xt/indexed-tx
    :with-tx-ops? true}
   (fn [tx]
     (when-not (:committed? tx)
       (let [ops (:xtdb.api/tx-ops tx)]
         (timbre/error "error committing operations: " (pr-str ops)))))))

(defmethod ig/halt-key! :infra.db/xtdb-listener [_ listener]
  (.close listener))
