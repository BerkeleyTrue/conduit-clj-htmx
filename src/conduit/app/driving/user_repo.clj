(ns conduit.app.driving.user-repo
  (:require
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.utils.xtdb :refer [node?]]))

(def transaction-functions
  [[:xt/put
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
    (first)))

(defact ->create-user
  [node]
  {:pre [(node? node)]}
  [{:keys [id email password username created-at]}]
  (xt/submit-tx node [[::xt/put
                       {:xt/id id
                        :user/username username
                        :user/email email
                        :user/password password
                        :user/created-at created-at}]])
  (xt/sync node)
  (get-by-email-query node email))

(defact ->get-by-email [node]
  {:pre [(node? node)]}
  [{:keys [email]}]
  (get-by-email-query node email))

(defact ->get-by-username [node]
  {:pre [(node? node)]}
  [{:keys [username]}]
  (let [query (xt/q
                (xt/db node)
                '{:find [(pull ?user [*])]
                  :where [[?user :user/username username]]
                  :in [username]}
                username)]
    (first query)))

(defact ->get-following [node]
  {:pre [(node? node)]}
  [{:keys [id]}]
  (let [query (xt/q
                (xt/db node)
                '{:find [(pull ?user [*])]
                  :where [[?user :user/following id]]
                  :in [id]}
                id)]
    (first query)))
(defact ->update [_] [])
(defact ->follow [_] [])
(defact ->unfollow [_] [])

(defmethod ig/init-key :app.repos/user [_ {:keys [conn]}]
  {:create-user (->create-user conn)
   :get-by-email (->get-by-email conn)
   :get-by-username (->get-by-username conn)
   :get-following (->get-following conn)
   :update (->update conn)
   :follow (->follow conn)
   :unfollow (->unfollow conn)})
