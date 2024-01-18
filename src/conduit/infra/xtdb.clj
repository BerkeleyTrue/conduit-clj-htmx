(ns conduit.infra.xtdb
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [taoensso.timbre :as timbre]
   [conduit.app.driving.user-repo :as user-repo]))


(defmethod ig/init-key :infra.db/xtdb [_ {:keys [index-dir doc-dir log-dir]}]
  (let [node (xt/start-node
              {:xtdb/tx-log {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                                        :db-dir (io/file log-dir)}}
               :xtdb/document-store {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                                                :db-dir (io/file doc-dir)}}
               :xtdb/index-store {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                                             :db-dir (io/file index-dir)}}})
        _ (xt/submit-tx node user-repo/transaction-functions)
        f (future (xt/sync node))]

    (while (not (realized? f))
      (Thread/sleep 2000)
      (when-some [indexed (xt/latest-completed-tx node)]
        (timbre/info "Indexed: " (pr-str indexed))))

    node))

(defmethod ig/halt-key! :infra.db/xtdb [_ node]
  (.close node))
