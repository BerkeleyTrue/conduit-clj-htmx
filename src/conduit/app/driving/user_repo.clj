(ns conduit.app.driving.user-repo
  (:require
   [datalevin.core :as d]
   [integrant.core :as ig]
   [conduit.utils.dep-macro :refer [defact]]))

(def user-schema
  {:user/email
   {:db/ident       :user/email
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   :user/username
   {:db/ident       :user/username
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}})

; TODO: return user
(defact ->create-user
  [conn]
  {:pre [(d/conn? conn)]}
  [{:keys [email password username created-at]}]
  (d/transact! conn [{:db/id -1
                      :user/username username
                      :user/email email
                      :user/password password
                      :user/created-at created-at}])
  (first (d/q '[:find ?e ?email ?username ?password ?created-at
                :in $ ?email
                :where
                [?e :user/email ?email]
                [?e :user/username ?username]
                [?e :user/password ?password]
                [?e :user/created-at ?created-at]]
              (d/db conn)
              email)))

(defact ->get-by-email [conn]
  {:pre [(d/conn? conn)]}
  [{:keys [email]}]
  (let [query (d/q '[:find ?e ?email ?username ?password ?created-at
                     :in $ ?email
                     :where
                     [?e :user/email ?email]
                     [?e :user/username ?username]
                     [?e :user/password ?password]
                     [?e :user/created-at ?created-at]]
                   (d/db conn)
                   email)]
    (first query)))

(defact ->get-by-username [conn]
  {:pre [(d/conn? conn)]}
  [{:keys [username]}]
  (let [query (d/q '[:find ?e ?email ?username ?password ?created-at
                     :in $ ?username
                     :where
                     [?e :user/email ?email]
                     [?e :user/username ?username]
                     [?e :user/password ?password]
                     [?e :user/created-at ?created-at]]
                   (d/db conn)
                   username)]
    (first query)))

(defact ->get-following [_] [])
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
