(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :as ig-repl :refer [halt reset]]
   [portal.api :as p]
   [conduit.config :refer [get-config]]
   [conduit.core]))

(ig-repl/set-prep! #(ig/prep (get-config)))

(defn run-config [key deps f]
  (let [system (ig/init (get-config) (conj deps key))
        dep (get system key)]
    (f dep)
    (ig/halt! system)))

(defn go []
  (ig-repl/go))

(comment
  (ig/prep (get-config))
  (ig-repl/go) ; starts the system
  (halt) ; stops the system
  (reset) ; resets the system
  (get-config)
  (ig/prep (get-config))
  (do 
    (add-tap #'p/submit)
    (p/open)
    (tap> :set))
  ,)
