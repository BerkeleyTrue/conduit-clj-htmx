(ns conduit.app.driving.user-repo
  (:require
   [integrant.core :as ig]
   [xtdb.api :as xt]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.utils.xtdb :refer [node?]])
  (:import [java.util UUID]))

(defact ->create-user
  [node]
  {:pre [(node? node)]}
  [{:keys [email password username created-at]}]
  (xt/submit-tx node [[::xt/put
                       {:xt/id email
                        :user/username username
                        :user/email email
                        :user/password password
                        :user/created-at created-at}]])
  (sync node)
  (first (xt/q
           (xt/db node)
           '[:find ?e ?email ?username ?password ?created-at
             :in $ ?email
             :where
             [?e :user/email ?email]
             [?e :user/username ?username]
             [?e :user/password ?password]
             [?e :user/created-at ?created-at]]
           email)))

(defact ->get-by-email [conn]
  {:pre [(node? conn)]}
  [{:keys [email]}]
  (let [query (xt/q '[:find ?e ?email ?username ?password ?created-at
                      :in $ ?email
                      :where
                      [?e :user/email ?email]
                      [?e :user/username ?username]
                      [?e :user/password ?password]
                      [?e :user/created-at ?created-at]]
                    (xt/db conn)
                    email)]
    (first query)))

(defact ->get-by-username [node]
  {:pre [(node? node)]}
  [{:keys [username]}]
  (let [query (xt/q '[:find ?e ?email ?username ?password ?created-at
                      :in $ ?username
                      :where
                      [?e :user/email ?email]
                      [?e :user/username ?username]
                      [?e :user/password ?password]
                      [?e :user/created-at ?created-at]]
                    (xt/db node)
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
