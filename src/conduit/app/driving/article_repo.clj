(ns conduit.app.driving.article-repo
  (:require
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [malli.core :as m]
   [conduit.core.models :refer [Article]]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.utils.malli]
   [conduit.utils.xtdb :refer [node?]]))

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
    {:article-id (:xt/id article)
     :title (:article/title article)
     :slug (:article/slug article)
     :description (:article/description article)
     :body (:article/body article)
     :author-id (:article/author-id article)
     :tags (:article/tags article)
     :created-at (:article/created-at article)
     :updated-at (:article/updated-at article)}))


(defact ->create-article
  [node]
  {:pre [(node? node)]}
  [{:keys [id title slug description body tags author-id created-at]}]
  (let [tx-res (xt/submit-tx node [[::xt/put
                                    {:xt/id id
                                     :article/title title
                                     :article/slug slug
                                     :article/description description
                                     :article/body body
                                     :article/tags tags
                                     :article/author-id author-id
                                     :article/created-at created-at}]])]
    (xt/await-tx node tx-res)
    (-> (xt/entity (xt/db node) id)
        (format-to-article))))

(defmethod ig/init-key :app.repos/article [_ {:keys [node]}]
  {:create (->create-article node)})
