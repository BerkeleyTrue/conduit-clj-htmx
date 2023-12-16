(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :as ig-repl :refer [go halt reset]]
   [conduit.config :refer [config]]
   [conduit.core]))

(ig-repl/set-prep! #(ig/prep config))

(comment
  (ig/prep config)
  (go) ; starts the system
  (halt) ; stops the system
  (reset)) ; resets the system
