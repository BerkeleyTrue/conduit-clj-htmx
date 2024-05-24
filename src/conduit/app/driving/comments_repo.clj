(ns conduit.app.driving.comments-repo
  (:require
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [malli.core :as m]
   [conduit.core.models :refer [Comment]]
   [conduit.utils.xtdb :refer [node?]]
   [conduit.core.ports.comment-repo :as repo]))

(def CommentEntity
  [:map
   [:xt/id :uuid]
   [:comment/id :uuid]
   [:comment/body :string]
   [:comment/article-id :uuid]
   [:comment/author-id :uuid]
   [:comment/created-at :instant]])

(def CommentInput
  [:map
   [:body :string]
   [:article-id :uuid]
   [:author-id :uuid]
   [:created-at :instant]])

(m/=> ->comment-put [:=> [:cat CommentInput] [:map [::xt/put CommentEntity]]])
(defn ->comment-put [{:keys [comment-id body author-id article-id created-at]}]
  [::xt/put {:xt/id comment-id
             :comment/id comment-id
             :comment/body body
             :comment/article-id article-id
             :comment/author-id author-id
             :comment/created-at created-at}])

(m/=> ->Comment [:=> [:cat [:maybe CommentEntity]] [:maybe Comment]])
(defn ->Comment [comment]
  (when-let [{:comment/keys [id article-id author-id body created-at]} comment]
    {:comment-id id
     :article-id article-id
     :author-id author-id
     :body body
     :created-at created-at}))
             

(defrecord CommentsRepo [node]
  repo/CommentRepository
  (create [_ {:keys [comment-id] :as params}] 
    (let [tx-res (xt/submit-tx node [(->comment-put params)])]
      (xt/await-tx node tx-res)
      (-> (xt/entity (xt/db node) comment-id)
          (->Comment))))

  (find-by-id [_ comment-id]
    (let [res (xt/entity (xt/db node) comment-id)]
      (->Comment res)))
  
  (list-by-article [_ article-id]
    (let [res (xt/q (xt/db node)
                    '{:find [(pull ?comment [*]) ?created-at]
                      :where [[?comment :comment/article-id article-id]
                              [?comment :comment/created-at ?created-at]]
                      :in [article-id]
                      :sort-by [[?created-at :desc]]}
                    article-id)]
      (->> res
        (mapv first)
        (mapv ->Comment))))

  (delete [_ comment-id]
    (let [tx-res (xt/submit-tx node [[::xt/delete comment-id]])]
      (xt/await-tx node tx-res)
      (-> (xt/entity (xt/db node) comment-id)
          (not)
          (boolean)))))

(defmethod ig/init-key :app.repos/comments [_ {:keys [node]}]
  (node? node)
  (->CommentsRepo node))
