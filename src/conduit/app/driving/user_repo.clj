(ns conduit.app.driving.user-repo
  (:require
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [malli.core :as m]
   [conduit.core.models :refer [User]]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.utils.malli]
   [conduit.utils.xtdb :refer [node?]]))

(def User-Entity
  [:map
   [:xt/id :string]
   [:user/email :email]
   [:user/username :string]
   [:user/bio {:optional true} [:maybe :string]]
   [:user/image {:optional true} [:maybe :string]]
   [:user/following [:set :uuid]]
   [:user/password :string]
   [:user/created-at :string]
   [:user/updated-at {:optional true} [:maybe :string]]])

(m/=> format-to-user [:=> [:cat [:maybe User-Entity]] [:maybe User]])
(defn format-to-user
  "formats a user entity to a domain user"
  [user]
  (if (nil? user)
    nil
    {:user-id (:xt/id user)
     :email (:user/email user)
     :username (:user/username user)
     :bio (:user/bio user)
     :image (:user/image user)
     :following (:user/following user)
     :password (:user/password user)

     :created-at (:user/created-at user)
     :updated-at (:user/updated-at user)}))

(def transaction-functions
  [[::xt/put
    {:xt/id :users/follow-author
     :xt/fn
     '(fn follow-author [ctx eid author-id]
        (let [db (xtdb.api/db ctx)
              user (xtdb.api/entity db eid)]
          [[::xt/put
            (-> user
                (update :user/following conj author-id)
                (assoc :user/updated-at (str (java.util.Date.))))]]))}]

   [::xt/put
    {:xt/id :users/unfollow-author
     :xt/fn
     '(fn unfollow-author [ctx eid author-id]
        (let [db (xtdb.api/db ctx)
              user (xtdb.api/entity db eid)]
          [[::xt/put
            (-> user
                (update :user/following disj author-id)
                (assoc :user/updated-at (str (java.util.Date.))))]]))}]

   [::xt/put
    {:xt/id :users/update
     :xt/fn
     '(fn update-user [ctx eid {:keys [email username bio image updated-at]}]
        (let [db (xtdb.api/db ctx)
              user (xtdb.api/entity db eid)]
          [[::xt/put
            (-> user
                (update :user/email (fn [old] (or email old)))
                (update :user/username (fn [old] (or username old)))
                (update :user/bio (fn [old] (or bio old)))
                (update :user/image (fn [old] (or image old)))
                (assoc :user/updated-at updated-at))]]))}]])

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
    (-> (xt/entity (xt/db node) id)
        (format-to-user))))

(defact ->get-by-id
  [node]
  {:pre [(node? node)]}
  [user-id]
  (-> (xt/entity (xt/db node) user-id)
      (format-to-user)))

(defact ->get-by-email [node]
  {:pre [(node? node)]}
  [email]
  (-> node
      (xt/db)
      (xt/q
       '{:find [(pull ?user [*])]
         :where [[?user :user/email email]]
         :in [email]}
       email)
      (first)
      (first)
      (format-to-user)))

(defact ->get-by-username [node]
  {:pre [(node? node)]}
  [username]
  (->
   (xt/db node)
   (xt/q
    '{:find [(pull ?user [*])]
      :where [[?user :user/username username]]
      :in [username]}
    username)
   (first)
   (first)
   (format-to-user)))

(defact ->update
  "Update user profile"
  [node]
  {:pre [(node? node)]}
  [{:keys [user-id email username bio image updated-at]}]
  (let [tx-res (xt/submit-tx node [[::xt/fn
                                    :users/update
                                    user-id
                                    {:email email
                                     :username username
                                     :bio bio
                                     :image image
                                     :updated-at updated-at}]])]
    (xt/await-tx node tx-res)
    (-> (xt/entity (xt/db node) user-id)
        (format-to-user))))

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
    (-> (xt/entity (xt/db node) user-id)
        (format-to-user))))

(defact ->unfollow
  "A user unfollows an author"
  [node]
  {:pre [(node? node)]}
  [{:keys [user-id author-id]}]
  (let [tx-res (xt/submit-tx node
                             [[::xt/fn
                               :users/unfollow-author
                               user-id
                               author-id]])]
    (xt/await-tx node tx-res)
    (-> (xt/entity (xt/db node) author-id)
        (format-to-user))))

(defact ->get-following
  "get a list of user ids that follow this author"
  [node]
  {:pre [(node? node)]}
  [{:keys [author-id]}]
  (-> (xt/q (xt/db node)
            '{:find [(pull ?user [*])]
              :where [[?e :xt/id]
                      [?e :following/author ?author-id]]
              :in [id]}
            author-id)
      (first)
      (format-to-user)))

(defmethod ig/init-key :app.repos/user [_ {:keys [node]}]
  {:create-user (->create-user node)
   :get-by-id (->get-by-id node)
   :get-by-email (->get-by-email node)
   :get-by-username (->get-by-username node)
   :get-following (->get-following node)
   :update (->update node)
   :follow (->follow node)
   :unfollow (->unfollow node)})
