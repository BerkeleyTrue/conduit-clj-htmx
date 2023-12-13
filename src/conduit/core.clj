(ns conduit.core
  (:require
    [aero.core :as aero]
    [integrant.core :as ig]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defn -main
  "I don't do a whole lot ... yet."
  [& _]
  (println "Hello, World!"))
