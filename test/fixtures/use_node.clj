(ns fixtures.use-node
  (:require
   [xtdb.api :as xt]
   [conduit.utils.dep-macro :refer [defact]]))

(def ^:dynamic *node* nil)

(defact ->db-fixture
  [txs]
  {:pre [(vector? txs)]}
  [f]
  (let [node (xt/start-node {})]
    (when (not-empty txs)
      (xt/await-tx node (xt/submit-tx node txs)))
    (binding [*node* node]
      (try
        (f)
        (finally
          (.close node))))
    (.close node)))
