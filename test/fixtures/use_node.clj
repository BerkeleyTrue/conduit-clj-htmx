(ns fixtures.use-node
  (:require
   [xtdb.api :as xt]))

(def ^:dynamic *node* nil)

(defn db-fixture [f]
  (let [node (xt/start-node {})]
    (binding [*node* node]
      (try
        (f)
        (finally
          (.close node))))
    (.close node)))
