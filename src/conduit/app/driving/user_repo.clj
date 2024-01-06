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

(defact ->create-user
  [conn]
  {:pre [(d/conn? conn)]}
  [{:keys [email password username created-at]}]
  (d/transact! conn [{:db/id -1
                      :user/username username
                      :user/email email
                      :user/password password
                      :user/created-at created-at}]))

(defn ->find-user-by-email [conn]
  {:pre [(d/conn? conn)]}
  (fn find-user-by-email [{:keys [email]}]
    (let [query (d/q '[:find ?e ?email ?username ?password ?created-at
                       :in $ ?email
                       :where
                       [?e :user/email ?email]
                       [?e :user/username ?username]
                       [?e :user/password ?password]
                       [?e :user/created-at ?created-at]]
                     (d/db conn)
                     email)]
      (first query))))

(defmethod ig/init-key :app.repos/user [_ {:keys [conn]}]
  (assert (d/conn? conn))
  {:create-user (->create-user conn)
   :find-user-by-email (->find-user-by-email conn)})
