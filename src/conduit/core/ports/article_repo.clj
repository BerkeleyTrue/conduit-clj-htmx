(ns conduit.core.ports.article-repo
  (:refer-clojure :exclude [list update]))

(defprotocol ArticleRepository
  (create [this params] "Create an article")
  (create-many [this params] "Create many articles")
  (get-by-id [this article-id] "Get an article by id")
  (get-by-slug [this slug] "Get and article by slug")
  (list [this params] "List articles")
  (get-popular-tags [this] "Get popular tags")
  (get-num-of-favorites [this article-id] "Get number of favorites")
  (is-favorited-by-user [this article-id user-id] "Check if article is favorited by user")
  (update [this article-id params] "Update an article")
  (favorite [this article-id user-id] "Favorite an article")
  (unfavorite [this article-id user-id] "Unfavorite an article")
  (delete [this article-id] "Delete an article"))

(defn repo? [repo]
  (satisfies? ArticleRepository repo))
