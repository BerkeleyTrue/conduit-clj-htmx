(ns conduit.app.driving.user-repo
  (:require
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [malli.core :as m]
   [conduit.core.models :refer [User]]
   [conduit.utils.xtdb :refer [node?]]
   [conduit.core.ports.user-repo :as user-repo]))

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

(def User-Entity
  [:map
   [:xt/id :uuid]
   [:user/id :uuid]
   [:user/email :email]
   [:user/username :string]
   [:user/bio {:optional true} [:maybe :string]]
   [:user/image {:optional true} [:maybe :string]]
   [:user/following [:set :uuid]]
   [:user/password :string]
   [:user/created-at :instant]
   [:user/updated-at {:optional true} [:maybe :instant]]])

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

(m/=> user->put [:=> [:cat User] :any])
(defn user->put
  "Convert a user entity to a query"
  [{:keys [user-id email password username created-at]}]
  [::xt/put
   {:xt/id user-id
    :user/id user-id
    :user/username username
    :user/email email
    :user/following #{}
    :user/password password
    :user/created-at created-at}])

(defrecord UserRepo [node]
  user-repo/UserRepository
  (create [_ {:keys [user-id] :as params}]
    (let [tx-res (xt/submit-tx node [(user->put params)])]
      (xt/await-tx node tx-res)
      (-> (xt/entity (xt/db node) user-id)
          (format-to-user))))

  (create-many [_ users]
    (let [tx-res (xt/submit-tx node (map user->put users))]
      (xt/await-tx node tx-res)
      (map (comp format-to-user (partial xt/entity (xt/db node)) :user-id) users)))

  (get-by-id [_ user-id]
    (-> (xt/entity (xt/db node) user-id)
        (format-to-user)))

  (get-by-email [_ email]
    (-> (xt/db node)
        (xt/q
         '{:find [(pull ?user [*])]
           :where [[?user :user/email email]]
           :in [email]}
         email)
        (first)
        (first)
        (format-to-user)))

  (get-by-username [_ username]
    (-> (xt/db node)
        (xt/q
         '{:find [(pull ?user [*])]
           :where [[?user :user/username username]]
           :in [username]}
         username)
        (first)
        (first)
        (format-to-user)))

  (get-following [_ user-id]
    (-> (xt/db node)
        (xt/q
         '{:find [(pull ?user [*])]
           :where [[?e :xt/id]
                   [?e :following/author ?author-id]]
           :in [id]}
         user-id)
        (first)
        (format-to-user)))

  (update [_ user-id params]
    (let [tx-res (xt/submit-tx
                  node
                  [[::xt/fn :users/update user-id params]])]
      (xt/await-tx node tx-res)
      (-> (xt/entity (xt/db node) user-id)
          (format-to-user))))

  (follow-author [_ user-id author-id]
    (let [tx-res (xt/submit-tx
                  node
                  [[::xt/fn
                    :users/follow-author
                    user-id
                    author-id]])]
      (xt/await-tx node tx-res)
      (-> (xt/db node)
          (xt/entity user-id)
          (format-to-user))))

  (unfollow-author [_ user-id author-id]
    (let [tx-res (xt/submit-tx node
                               [[::xt/fn
                                 :users/unfollow-author
                                 user-id
                                 author-id]])]
      (xt/await-tx node tx-res)
      (-> (xt/db node)
          (xt/entity author-id)
          (format-to-user)))))

(defmethod ig/init-key :app.repos/user [_ {:keys [node]}]
  (node? node)
  (->UserRepo node))
