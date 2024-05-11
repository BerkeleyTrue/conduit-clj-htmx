(ns conduit.app.driving.article-repo
  (:require
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [malli.core :as m]
   [conduit.core.models :refer [Article]]
   [conduit.utils.xtdb :refer [node?]]
   [conduit.core.ports.article-repo :as article-repo]))

(def ArticleEntity
  [:map
   [:xt/id :string]
   [:article/title :string]
   [:aritcle/slug :string]
   [:article/description :string]
   [:article/body :string]
   [:article/author-id :string]
   [:article/tags [:set :string]]
   [:article/created-at :instant]
   [:article/updated-at {:optional true} [:maybe :instant]]])

(m/=> format-to-article [:=> [:cat [:maybe ArticleEntity]] [:maybe Article]])
(defn format-to-article
  "formats an article entity to a domain article"
  [article]
  (if (nil? article)
    nil
    {:article-id (:xt/id article)
     :title (:article/title article)
     :slug (:article/slug article)
     :description (:article/description article)
     :body (:article/body article)
     :author-id (:article/author-id article) 
     :tags (:article/tags article)
     :created-at (:article/created-at article)
     :updated-at (:article/updated-at article)}))

(m/=> article->put [:=> [:cat Article] :any])
(defn article->put [{:keys [article-id title slug description body tags author-id created-at]}]
  [::xt/put
   {:xt/id article-id
    :article/id article-id
    :article/title title
    :article/slug slug
    :article/description description
    :article/body body
    :article/tags tags
    :article/author-id author-id
    :article/created-at created-at}])

(defrecord ArticleRepo [node]
  article-repo/ArticleRepository
  (create [_ article]
    (let [tx-res (xt/submit-tx node [(article->put article)])]
      (xt/await-tx node tx-res)
      (-> (xt/entity (xt/db node) (:article-id article))
          (format-to-article))))

  (create-many [_ articles]
    (let [tx-res (xt/submit-tx node (map article->put articles))]
      (xt/await-tx node tx-res)
      (map (comp (partial xt/entity (xt/db node)) :article-id)
           articles)))

  ; TODO: figure out followed-by
  (list [_ {:keys [limit offset _followed-by]}]
    (let [res (xt/q (xt/db node) 
                    {:find '[(pull ?article [*])]
                     :where '[[?article :article/title ?id]]
                     :limit limit
                     :offset offset})
          res (->>
                res
                (flatten)
                (map format-to-article))]
      res))
  (get-popular-tags [_]
    (let [res (xt/q (xt/db node)
                    '{:find [(count ?article) ?tags]
                      :where [[?article :article/tags ?tags]]
                      :order-by [[(count ?article) :desc] [?tags :asc]]
                      :limit 10})
          res (->> res
                  (map (fn [[_ tag]] tag)))]
      res)))


(defmethod ig/init-key :app.repos/article [_ {:keys [node]}]
  (node? node)
  (->ArticleRepo node))
