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
   [:xt/id :uuid]
   [:article/title :string]
   [:article/slug :string]
   [:article/description :string]
   [:article/body :string]
   [:article/author-id :uuid]
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

  (list [_ {:keys [limit offset tag authorname followed-by _favorited-by]}]
    (let [db (xt/db node)
          res (if followed-by
                (xt/q db
                      {:find '[(pull ?article [*])]
                       :where '[[?article :article/id ?article-id]
                                [?user :user/id user-id]
                                [?article :article/author-id ?author-id]
                                [?user :user/following ?author-id]]
                       :in '[user-id]
                       :limit limit
                       :offset offset}
                      followed-by)

                (xt/q db
                      {:find '[(pull ?article [*]) ?created-at (distinct ?title)]
                       :where '[[?article :article/title ?title]
                                [?article :article/tags ?tags] ; tags are unrolled, so we need distinct by title to reduce duplicates
                                [?article :article/author-id ?author-id]
                                [?article :article/created-at ?created-at]
                                [?user :user/id ?author-id]
                                [?user :user/username ?username]
                                (or [(not tag)] 
                                    [(= tag ?tags)])
                                (or [(not authorname)] 
                                    [(= authorname ?username)])]
                                
                       :in '[tag authorname]
                       :limit limit
                       :offset offset
                       :order-by '[[?created-at :desc]]}
                      tag
                      authorname))
                      
          count (if followed-by
                  (xt/q db 
                        '{:find [(count ?article)]
                          :where [[?article :article/title]
                                  [?user :user/id user-id]
                                  [?article :article/author-id ?author-id]
                                  [?user :user/following ?author-id]]
                          :in [user-id]}
                        followed-by)
                  (xt/q db
                        '{:find [(count ?article) (distinct ?title)]
                          :where [[?article :article/title ?title]
                                  [?article :article/tags ?tags] ; tags are unrolled, so we need distinct by title to reduce duplicates
                                  [?article :article/author-id ?author-id]
                                  [?article :article/created-at ?created-at]
                                  [?user :user/id ?author-id]
                                  [?user :user/username ?username]
                                  (or [(not tag)] 
                                      [(= tag ?tags)])
                                  (or [(not authorname)] 
                                      [(= authorname ?username)])]
                                
                          :in [tag authorname]}
                        tag
                        authorname))
                        
          num-of-articles (-> count 
                              (first) 
                              (first))

          articles (->> res
                        (map first)
                        (flatten)
                        (map format-to-article))]

      {:articles articles
       :num-of-articles num-of-articles
       :page (+ (/ offset limit) 1)}))

  (get-popular-tags [_]
    (let [res (xt/q (xt/db node)
                    '{:find [(count ?article) ?tags]
                      :where [[?article :article/tags ?tags]]
                      :order-by [[(count ?article) :desc] [?tags :asc]]
                      :limit 10})]
      (map (fn [[_ tag]] tag) res)))
  
  (get-by-id [_ article-id]
    (let [res (xt/entity (xt/db node) article-id)]
      (-> res
          (first)
          (first)
          (format-to-article))))

  (get-by-slug [_ slug]
    (let [res (xt/q (xt/db node)
                    '{:find [(pull ?article [*])]
                      :in [slug]
                      :where [[?article :article/slug slug]]}
                    slug)]
      (-> res
          (first)
          (first)
          (format-to-article)))))

(defmethod ig/init-key :app.repos/article [_ {:keys [node]}]
  (node? node)
  (->ArticleRepo node))
