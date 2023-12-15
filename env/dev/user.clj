(ns user
  (:require
   [integrant.core :as ig]
   [integrant.repl :as ig-repl :refer [go halt reset]]
   [conduit.config :refer [config]]
   [conduit.core :refer [start-app]]))

(ig-repl/set-prep! #(ig/prep (:server config)))

(comment
  (go) ; starts the system
  (halt) ; stops the system
  (reset)) ; resets the system
