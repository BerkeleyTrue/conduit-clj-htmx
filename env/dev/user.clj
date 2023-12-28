(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :as ig-repl :refer [go halt reset]]
   [reitit.core :as r]
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
  (let [x (:infra.router/core (ig/init (get-config) [:infra.router/routes
                                                     :infra.router/core]))]
    (r/options x))
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
