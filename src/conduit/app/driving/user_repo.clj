(ns conduit.app.driving.user-repo
  (:require
   [datalevin.core :as d]
   [integrant.core :as ig]))

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

(defn ->create-user [conn]
  (fn create-user [{:keys [email password username created-at]}]
    (d/transact!
     conn
     [{:db/add -1
       :user/username username
       :user/email email
       :user/password password
       :user/created-at created-at}])))

(defn ->find-user-by-email [conn]
  (fn find-user-by-email [{:keys [email]}]
    (let [query (d/q '[:find ?e
                       :in $ ?email
                       :where
                       [?e :user/email ?email]]
                     (d/db conn)
                     email)]
      (first query))))


(defmethod ig/init-key :app.repos/user [_ {:keys [conn]}]
  {:create-user (->create-user conn)
   :find-user-by-email (->find-user-by-email conn)})
