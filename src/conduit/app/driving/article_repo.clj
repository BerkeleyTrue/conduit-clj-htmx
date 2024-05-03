(ns conduit.app.driving.article-repo
  (:require
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [malli.core :as m]
   [conduit.core.models :refer [Article]]
   [conduit.utils.malli]
   [conduit.utils.xtdb :refer [node?]]
   [conduit.core.ports.article-repo :as article-repo])
  (:import
   [java.util UUID]))

(def Article-Entity
  [:map
   [:xt/id :string]
   [:article/title :string]
   [:aritcle/slug :string]
   [:article/description :string]
   [:article/body :string]
   [:article/author-id :string]
   [:article/tags [:set :string]]
   [:article/created-at :string]
   [:article/updated-at {:optional true} [:maybe :string]]])

(m/=> format-to-article [:=> [:cat [:maybe Article-Entity]] [:maybe Article]])
(defn format-to-article
  "formats an article entity to a domain article"
  [article]
  (if (nil? article)
    nil
    {:article-id (UUID/fromString (:xt/id article))
     :title (:article/title article)
     :slug (:article/slug article)
     :description (:article/description article)
     :body (:article/body article)
     :author-id (when-let [author-id (:article/author-id article)] 
                  (UUID/fromString author-id))
     :tags (:article/tags article)
     :created-at (:article/created-at article)
     :updated-at (:article/updated-at article)}))

(defn article->put [{:keys [id title slug description body tags author-id created-at]}]
  [::xt/put
   {:xt/id id
    :article/title title
    :article/slug slug
    :article/description description
    :article/body body
    :article/tags tags
    :article/author-id author-id
    :article/created-at created-at}])

(defrecord ArticleRepo [node]
  article-repo/ArticleRepository
  (create [_ params]
    (let [tx-res (xt/submit-tx node [(article->put params)])]
      (xt/await-tx node tx-res)
      (-> (xt/entity (xt/db node) (:id params))
          (format-to-article))))

  (create-many [_ articles]
    (let [tx-res (xt/submit-tx node (map article->put articles))]
      (xt/await-tx node tx-res)
      (map (comp (partial xt/entity (xt/db node)) :id)
           articles)))

  (list [_ {:keys [limit offset followed-by]}]
    (let [res (xt/q (xt/db node) 
                    '{:find [(pull ?article [*])]
                      :where [[?article :article/title ?id]]
                      :limit 10
                      :offset 0})
          res (->>
                res
                (flatten)
                (map format-to-article))]
      res)))


(defmethod ig/init-key :app.repos/article [_ {:keys [node]}]
  (node? node)
  (->ArticleRepo node))
