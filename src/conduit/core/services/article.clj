(ns conduit.core.services.article
  (:require
   [integrant.core :as ig]
   [conduit.core.ports.article-repo :as article-repo]
   [java-time.api :as jt]))

(defprotocol ArticleService
  (create [_ user-id params] "Create an article")
  (list-articles [_ params] "List articles")
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
  (article-repo/repo? repo)
  (reify ArticleService
    (create [_ user-id params]
      (let [article (article-repo/create repo 
                                         (assoc params 
                                                :created-at (str (jt/instant))
                                                :author-id user-id))] 
        (if (nil? article)
          {:error "Couldn't create article"}
          (let [user (get-profile {:user-id user-id})]
            {:user (format-article article user 0 false)}))))
    (list [_ user-id params]
      (let [params (or )]))))
