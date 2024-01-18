(ns conduit.utils.xtdb
  ; needs to happen after xtdb.node is imported
  ; in order to for the underlying class to be compiled
  (:require [xtdb.node])
  (:import [xtdb.node XtdbNode]))

(defn node? [?node]
  (instance? XtdbNode ?node))
