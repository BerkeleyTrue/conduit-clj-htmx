(ns conduit.core.services.article
  (:refer-clojure :exclude [list update])
  (:require
   [java-time.api :as jt]
   [integrant.core :as ig]
   [malli.core :as m]
   [conduit.core.ports.article-repo :as repo]
   [conduit.core.services.user :refer [User-Profile]]
   [conduit.core.models :refer [Article]]))

(def Article-Output
  [:map
   {:title "Article Output"
    :description "An public article"}
   [:title :string]
   [:slug :string]
   [:description :string]
   [:body :string]
   [:tags [:set :string]]

   [:is-favorited :boolean]
   [:favorites-count :int]
   [:author User-Profile]

   [:created-at :string]
   [:updated-at :string]])

(defprotocol ArticleService
  (create [_ user-id params] "Create an article")
  (list [_ user-id params] "List articles")
  (get-popular-tags [_] "Get popular tags")
  (get-by-slug [_ slug] "Get an article by slug")
  (get-id-from-slug [_ slug] "Get an article id by slug")
  (update-article [_ slug username params] "Update an article")
  (favorite [_ slug user-id] "Favorite an article")
  (unfavorite [_ slug user-id] "Unfavorite an article")
  (delete [_ slug] "Delete an article"))

(m/=> format-article [:=> [:cat Article User-Profile :int :boolean] Article-Output])
(defn format-article [article profile num-of-favorites favorited-by-user]
  {:slug (:article/slug article)
   :title (:title article)
   :description (:description article)
   :body (:body article)
   :tags (:tags article)
   :author profile
   :favorited favorited-by-user
   :favoritesCount num-of-favorites

   :created-at (:created-at article)
   :updated-at (:updated-at article)})

(defmethod ig/init-key :core.services/article [_ {repo :repo
                                                  {:keys [get-profile]} :user-service}]
  (repo/repo? repo)
  (reify ArticleService
    (create [_ user-id params]
      (let [article (repo/create repo
                                 (assoc params
                                        :created-at (str (jt/instant))
                                        :author-id user-id))]
        (if (nil? article)
          {:error "Couldn't create article"}
          (let [user (get-profile {:user-id user-id})]
            {:user (format-article article user 0 false)}))))

    (list [_ user-id {:keys [feed? limit offset tag _favorited _authorname]}]
      ; TODO: add fetch author-id for authorname
      ; TODO: add fetch userid for favorited
      (let [args (if feed?
                   {:followed-by user-id}
                   {:tag tag})]
        (->>
          (assoc args :limit (or limit 10) :offset (or offset 0))
          (repo/list repo)
          (map (fn [article]
                 ; TODO: num-of-favorites 
                 ; TODO: is favorited
                 (let [profile (get-profile (:author-id article))]
                   (format-article article profile (rand-int 10) (rand-nth [true false]))))))))))
