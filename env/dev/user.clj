(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :as ig-repl :refer [go halt reset]]
   [nextjournal.beholder :as beholder]
   [conduit.config :refer [get-config]]
   [conduit.core]))

(ig-repl/set-prep! #(ig/prep (get-config)))
(def watcher (atom nil))

(comment
  (ig/prep (get-config))
  (go) ; starts the system
  (halt) ; stops the system
  (reset) ; resets the system
  (get-config)
  (ig/prep (get-config))
  (swap!
    watcher
    (fn [old]
      (when (not (nil? old))
        (beholder/stop old))
      (beholder/watch
       (fn [_e]
         (reset)) "src")))
  (beholder/stop @watcher)
  ,)
