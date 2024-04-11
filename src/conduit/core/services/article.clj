(ns conduit.core.services.article
  (:require
   [integrant.core :as ig]
   [conduit.core.ports.article-repo :as repo]
   [java-time.api :as jt]))

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

(defn format-article [article profile num-of-favorites favorited-by-user]
  {:slug (:slug article)
   :title (:title article)
   :description (:description article)
   :body (:body article)
   :tag (:tags article)
   :author profile
   :favorited favorited-by-user
   :favoritesCount num-of-favorites

   :createdAt (:created-at article)
   :updatedAt (:updated-at article)})

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

    (list [_ user-id {:keys [feed? limit offset tag favorited authorname]}]
      ; TODO: add fetch authorid for authorname
      ; TODO: add fetch userid for favorited
      (let [args (if feed?
                   {:followed-by user-id}
                   {:tag tag})]
        (->>
          (repo/list repo args)
          (map (fn [article]
                 ; TODO: num-of-favorites 
                 ; TODO: is favorited
                 (let [profile (get-profile (:author-id article))]
                   (format-article article profile (rand-int 10) (rand-nth [true false]))))))))))
