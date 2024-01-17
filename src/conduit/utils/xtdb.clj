(ns conduit.utils.xtdb
  (:import [xtdb.node XtdbNode])
  (:require
    [xtdb.node]))

(defn node? [?node]
  (instance? XtdbNode ?node))
