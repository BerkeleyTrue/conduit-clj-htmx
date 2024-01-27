(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :as ig-repl :refer [go halt reset]]
   [dev.nu.morse :as morse]
   [conduit.config :refer [get-config]]
   [conduit.core]))

(ig-repl/set-prep! #(ig/prep (get-config)))
(def watcher (atom nil))

(defn run-config [key deps f]
  (let [system (ig/init (get-config) (conj deps key))
        dep (get system key)]
    (f dep)
    (ig/halt! system)))

(comment
  (ig/prep (get-config))
  (go) ; starts the system
  (halt) ; stops the system
  (reset) ; resets the system
  (morse/launch-in-proc)
  (get-config)
  (ig/prep (get-config))
  ,)
