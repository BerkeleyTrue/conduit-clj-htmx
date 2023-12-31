(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :as ig-repl :refer [go halt reset]]
   [nextjournal.beholder :as beholder]
   [datalevin.core :as d]
   [conduit.config :refer [get-config]]
   [conduit.core]
   [taoensso.timbre :as timbre]))

(ig-repl/set-prep! #(ig/prep (get-config)))
(def watcher (atom nil))

(comment
  (ig/prep (get-config))
  (go) ; starts the system
  (halt) ; stops the system
  (reset) ; resets the system
  (get-config)
  (ig/prep (get-config))
  (let [x (:infra.db/datalevin (ig/init (get-config) [:infra.db/datalevin]))]
    (println (d/schema x))
    (d/close x))
  (swap!
    watcher
    (fn [old]
      (when (not (nil? old))
        (beholder/stop old))
      (beholder/watch
       (fn [_e]
         (reset)) "src")))
  (beholder/stop @watcher)
  (let [db (:infra.db/datalevin-kv
             (ig/init (get-config)
                      [:infra.middleware.session/datalevin
                       :infra.db/datalevin-kv]))]
    (d/clear-dbi db "session-store")
    (timbre/info "val: " (d/get-value db "foo" :transacted nil)))
  ,)
