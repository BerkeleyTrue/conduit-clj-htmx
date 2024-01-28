(ns conduit.core.services.user
  (:require
   [conduit.utils.auth :as auth]
   [integrant.core :as ig]
   [conduit.utils.dep-macro :refer [defact]])
  (:import
   [java.util UUID Date]))

(defact ->register [{:keys [create-user get-by-email]}]
  {:pre [(fn? create-user) (fn? get-by-email)]}
  [{:keys [username email password]}]
  (let [user (get-by-email email)]
    (if (not (nil? user))
      {:error "Email is alread in use"}
      (let [hashed-password (auth/hash-password password)
            id (str (UUID/randomUUID))
            user (create-user {:id id
                               :username username
                               :email email
                               :password hashed-password
                               :created-at (str (Date.))})]
        (if (nil? user)
          {:error "Couldn't create user with that email and password"}
          {:user user})))))

(defact ->login [{:keys [get-by-email]}]
  {:pre [(fn? get-by-email)]}
  [{:keys [email password]}]
  (let [user (get-by-email email)]
    (if (and user (auth/verify-password password (:password user)))
      {:user user}
      {:error "No user with that email and password was found"})))

(defact ->find-user
  "Find user by id, email, or username."
  [{:keys [get-by-id get-by-email get-by-username]}]
  {:pre [(fn? get-by-id) (fn? get-by-email) (fn? get-by-username)]}
  [{:keys [user-id username email]}]
  (let [user (cond
               user-id (get-by-id user-id)
               username (get-by-username username)
               email (get-by-email email))]
    (if (nil? user)
      {:error "No user found"}
      {:user user})))

(defact ->update
  [{update-user :update}]
  {:pre [(fn? update-user)]}
  [params]
  (if (nil? (:user-id params))
    {:error "User id or username is required"}
    (let [now (str (Date.))
          params (if (empty? (:password params))
                   (assoc params :password (auth/hash-password (:password params)))
                   (dissoc params :password))
          user (update-user (assoc params :updated-at now))]
      (if (nil? user)
        {:error "Couldn't update user"}
        {:user user}))))

(defact ->get-profile [_] [])
(defact ->get-following [_] [])
(defact ->follow [_] [])
(defact ->unfollow [_] [])

(defmethod ig/init-key :core.services/user [_ {:keys [user-repo]}]
  {:register (->register user-repo)
   :login (->login user-repo)
   :find-user (->find-user user-repo)
   :get-profile (->get-profile user-repo)
   :get-following (->get-following user-repo)
   :follow (->follow user-repo)
   :unfollow (->unfollow user-repo)
   :update (->update user-repo)})
