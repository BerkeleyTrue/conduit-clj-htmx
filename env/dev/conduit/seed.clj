(ns conduit.seed
  (:require
    [conduit.config :refer [get-config]]
    [conduit.infra.datalevin :as d]))


(def config (get-config))
(def num-of-users 30)
(def num-of-articles 20)

(defn generate-image []
  (str "https://picsum.photos/id/" (rand-int 450) "/200/200"))

(defn generate-article [])

(defn generate-user [])

(defn clear-db []
  (println "Clearing database..."))

(defn seed [])


(defn start-seed []
  (clear-db)
  (println "Starting seed..."))
