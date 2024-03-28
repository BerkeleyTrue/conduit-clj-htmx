(ns conduit.seed
  (:require
   [clojure.string :as str]
   [camel-snake-kebab.core :as csk]
   [lambdaisland.faker :refer [fake]]
   [integrant.core :as ig]
   [java-time.api :as jt]
   [conduit.config :refer [get-config]])
  (:import
   [java.util UUID]))

(def config (get-config))
(def num-of-users 30)
(def num-of-articles 20)

(defn random-int [min max]
  (+ (rand-int (- max min)) min))

(comment
  (random-int 4 20))

(defn random-date []
  (let [start-ms (jt/to-millis-from-epoch (jt/zoned-date-time 2020))  ; 1577836800000
        end-ms (jt/to-millis-from-epoch (jt/zoned-date-time)) ; 1711473904654
        rand-ms (long (rand (- end-ms start-ms)))
        out-ms (+ start-ms rand-ms)]
    (jt/instant out-ms)))


(defn generate-image []
  (str "https://picsum.photos/id/" (rand-int 300) "/200/200"))

(defn words [] (repeatedly #(fake [:hipster :words])))
(defn sentence [] (str (str/join " " (vec (take (random-int 4 12) (words)))) "."))
(defn paragraph [] (str/join "\n" (vec (take (random-int 4 12) (repeatedly #(sentence))))))

(defn generate-user []
  (fake 
    {:id (str (UUID/randomUUID))
     :email [:internet :email]
     :username [:internet :username]
     :created-at (random-date)
     :password #"[a-zA-z0-9]{8,16}"}))

(defn generate-article [user]
  (let [title (str/join " " (vec (take (random-int 3 7) (words))))
        slug (csk/->kebab-case title)
        body (str/join "\n\n" (vec (take (random-int 1 5) (repeatedly #(paragraph)))))
        tags (vec (take (random-int 1 3) (words)))
        article (fake {:id (str (UUID/randomUUID))

                       :title title
                       :slug slug
                       :description (str/join "\n\n" (vec (take (random-int 1 4) (repeatedly #(sentence)))))
                       :body body

                       :image (generate-image)
                       :author-id (:id user)

                       :created-at (random-date)
                       :updated-at (random-date)})]
    (assoc article :tags tags)))

(comment
  (generate-article (generate-user)))

(defn clear-db []
  (println "Clearing database..."))

(defn seed [])

(defn start-seed []
  (println "Starting seed...")
  (->
    {}
    (ig/prep)
    (ig/init)))
