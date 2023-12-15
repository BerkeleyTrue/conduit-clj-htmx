(ns user
  (:require
   [aero.core :as aero]
   [integrant.core :as ig]
   [integrant.repl :as ig-repl :refer [go halt reset]]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(def config (:server (aero/read-config "config.edn")))

(ig-repl/set-prep! #(ig/prep config))

(comment
  (go) ; starts the system
  (halt) ; stops the system
  (reset)) ; resets the system
