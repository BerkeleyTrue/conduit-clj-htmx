(ns conduit.app.driving.user-repo
  (:require
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.utils.xtdb :refer [node?]]))

(def transaction-functions
  [[::xt/put
    {:xt/id :users/follow-author
     :xt/fn
     '(fn follow-author [ctx eid author-id]
        (let [db (xtdb.api/db ctx)
              user (xtdb.api/entity db eid)]
          [[::xt/put (update user :user/following conj author-id)]]))}]

   [::xt/put
    {:xt/id :users/unfollow-author
     :xt/fn
     '(fn unfollow-author [ctx eid author-id]
        (let [db (xtdb.api/db ctx)
              user (xtdb.api/entity db eid)]
          [[::xt/put (update user :user/following disj author-id)]]))}]

   [::xt/put
    {:xt/id :users/update
     :xt/fn
     '(fn update [ctx eid email username bio image]
        (let [db (xtdb.api/db ctx)
              user (xtdb.api/entity db eid)]
          [[::xt/put
            (->
              user
              (update :user/email (or email (:email user)))
              (update :user/username (or username (:username user)))
              (update :user/bio (or bio (:bio user)))
              (update :user/image (or image (:image user))))]]))}]])

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
                                     :user/following #{}
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
    (first)
    (first)))

(defact ->update
  "Update user profile"
  [node]
  {:pre [(node? node)]}
  [{:keys [id email username bio image]}]
  (let [tx-res (xt/submit-tx
                 node
                 [[::xt/fn
                   :users/update
                   id
                   email
                   username
                   bio
                   image]])]
    (xt/await-tx node tx-res)
    (xt/entity (xt/db node) id)))

(defact ->follow
  "A user follows an author"
  [node]
  {:pre [(node? node)]}
  [{:keys [user-id author-id]}]
  (let [tx-res (xt/submit-tx
                 node
                 [[::xt/fn
                   :users/follow-author
                   user-id
                   author-id]])]
    (xt/await-tx node tx-res)
    (xt/entity (xt/db node) user-id)))

(defact ->unfollow
  "A user unfollows an author"
  [node]
  {:pre [(node? node)]}
  [{:keys [user-id author-id]}]
  (let [tx-res (xt/submit-tx
                 node
                 [[::xt/fn
                   :users/unfollow-author
                   user-id
                   author-id]])]
    (xt/await-tx node tx-res)
    (xt/entity (xt/db node) author-id)))

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

(defmethod ig/init-key :app.repos/user [_ {:keys [node]}]
  {:create-user (->create-user node)
   :get-by-email (->get-by-email node)
   :get-by-username (->get-by-username node)
   :get-following (->get-following node)
   :update (->update node)
   :follow (->follow node)
   :unfollow (->unfollow node)})
