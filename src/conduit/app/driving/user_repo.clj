(ns conduit.app.driving.user-repo
  (:require
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.utils.xtdb :refer [node?]]))

(def transaction-functions
  [[::xt/put
    {:xt/id :add-to-following
     :xt/fn
     '(fn add-to-following [ctx eid new-follower]
        (let [db (xtdb.api/db ctx)
              user (xtdb.api/entity db eid)]
          [[::xt/put (update user :following conj follower)]]))}]])

(defn get-by-email-query
  "query user by email"
  [node email]
  (->
    node
    (xt/db)
    (xt/q
      '{:find [(pull ?user [*])]
        :where [[?user :user/email email]]
        :in [email]}
      email)
    (first)
    (first)))

(defact ->create-user
  [node]
  {:pre [(node? node)]}
  [{:keys [id email password username created-at]}]
  (let [tx-res (xt/submit-tx node [[::xt/put
                                     {:xt/id id
                                      :user/username username
                                      :user/email email
                                      :user/password password
                                      :user/created-at created-at}]])]
    (xt/await-tx node tx-res)
    (xt/entity (xt/db node) id)))

(defact ->get-by-email [node]
  {:pre [(node? node)]}
  [{:keys [email]}]
  (get-by-email-query node email))

(defact ->get-by-username [node]
  {:pre [(node? node)]}
  [{:keys [username]}]
  (->
    (xt/db node)
    (xt/q
      '{:find [(pull ?user [*])]
        :where [[?user :user/username username]]
        :in [username]}
      username)
    (first)))

(defact ->get-following
  "get a list of user ids that follow this author"
  [node]
  {:pre [(node? node)]}
  [{:keys [author-id]}]
  (let [query (xt/q
                (xt/db node)
                '{:find [(pull ?user [*])]
                  :where [[?e :xt/id]
                          [?e :following/author ?author-id]]
                  :in [id]}
                author-id)]
    (first query)))
(defact ->update [_] [])
(defact ->follow [_] [])
(defact ->unfollow [_] [])

(defmethod ig/init-key :app.repos/user [_ {:keys [node]}]
  {:create-user (->create-user node)
   :get-by-email (->get-by-email node)
   :get-by-username (->get-by-username node)
   :get-following (->get-following node)
   :update (->update node)
   :follow (->follow node)
   :unfollow (->unfollow node)})
