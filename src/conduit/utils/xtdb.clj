(ns conduit.utils.xtdb
  (:import
    [xtdb.api IXtdb])
  (:require
    [taoensso.timbre :as timbre]))

(defn xtdb? [node?]
  (timbre/info "Checking if node is an xtdb instance: " (pr-str node?))
  (instance? IXtdb node?))
