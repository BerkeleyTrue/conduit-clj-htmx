(ns fixtures.use-node
  (:require
   [xtdb.api :as xt]
   [conduit.utils.dep-macro :refer [defact]]))

(def ^:dynamic *node* nil)

(defact ->db-fixture
  [txs]
  {:pre [(vector? txs)]}
  [f]
  (let [node (xt/start-node {})
        listener (xt/listen
                   node
                   {::xt/event-type ::xt/indexed-tx
                    :with-tx-ops? true}
                   (fn [tx]
                     (when-not (:committed? tx)
                       (let [ops (:xtdb.api/tx-ops tx)]
                         (println "error committing operations: " (pr-str ops))))))]
    (when (not-empty txs)
      (xt/await-tx node (xt/submit-tx node txs)))
    (binding [*node* node]
      (try
        (f)
        (finally
          (.close listener)
          (.close node))))
    (.close listener)
    (.close node)))
