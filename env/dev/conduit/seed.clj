(ns conduit.seed
  (:require
   [clojure.string :as str]
   [java-time.api :as jt]
   [camel-snake-kebab.core :as csk]
   [lambdaisland.faker :refer [fake]]
   [integrant.core :as ig]
   [babashka.fs :as fs]
   [xtdb.api :as xt]
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

(defmethod ig/init-key :seed/clear [_ _]
  (println "Clearing database...")
  (fs/delete-tree "data")
  true)

(defmethod ig/init-key :seed/generate [_ {{:keys [create-many-users]} :user
                                          {:keys [create-many-articles]} :article
                                          node :node}]
  (println "Generating seed data...")
  (let [users (->> (repeatedly generate-user)
                   (take num-of-users)
                   (vec)
                   (create-many-users))
        [user-count]   (first (xt/q (xt/db node) '{:find [(count ?users)]
                                                   :where [[?users :user/email]]}))]

    (println "Generated" user-count "users")))

(defn start-seed []
  (println "Starting seed...")
  (->
   {:infra.db/xtdb (assoc (:infra.db/xtdb config) :clear (ig/ref :seed/clear))

    :app.repos/user {:node (ig/ref :infra.db/xtdb)}
    :app.repos/article {:node (ig/ref :infra.db/xtdb)}
    :seed/clear {}
    :seed/generate {:node (ig/ref :infra.db/xtdb)
                    :user (ig/ref :app.repos/user)
                    :article (ig/ref :app.repos/article)}}
   (ig/prep)
   (ig/init)))

(comment
  (start-seed))
