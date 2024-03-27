(ns conduit.seed
  (:require
   [conduit.config :refer [get-config]]
   [lambdaisland.faker :refer [fake]]
   [java-time.api :as jt])
  (:import
   [java.util UUID]))

(def config (get-config))
(def num-of-users 30)
(def num-of-articles 20)

(defn random-date []
  (let [start-ms (jt/to-millis-from-epoch (jt/zoned-date-time 2020))  ; 1577836800000
        end-ms (jt/to-millis-from-epoch (jt/zoned-date-time)) ; 1711473904654
        rand-ms (long (rand (- end-ms start-ms)))
        out-ms (+ start-ms rand-ms)]
    (jt/instant out-ms)))


(defn generate-image []
  (str "https://picsum.photos/id/" (rand-int 450) "/200/200"))

(defn generate-article [user]
  (fake
    {:id (str (UUID/randomUUID))
     :title [:lorem :words 3]
     :slug [:lorem :words 3]
     :description [:lorem :sentence]
     :body [:lorem :paragraphs 3]
     :tag-list (vec (take 3 (repeatedly #(fake [:lorem :words 1]))))
     :created-at (random-date)
     :updated-at (random-date)
     :favorited? (rand-nth [true false])
     :favorites-count (rand-int 100)
     :author (:id user)
     :image (generate-image)}))

(defn generate-user []
  (fake 
    {:id (str (UUID/randomUUID))
     :email [:internet :email]
     :username [:internet :username]
     :created-at (random-date)
     :password #"[a-zA-z0-9]{8,16}"}))

(defn clear-db []
  (println "Clearing database..."))

(defn seed [])

(defn start-seed []
  (clear-db)
  (println "Starting seed..."))
