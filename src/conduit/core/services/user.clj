(ns conduit.core.services.user
  (:require
   [conduit.utils.auth :as auth]
   [integrant.core :as ig]
   [conduit.utils.dep-macro :refer [defact]]))

(defact ->register [{:keys [create-user get-by-email]}]
  {:pre [(fn? create-user)]}
  [{:keys [username email password]}]
  (let [user (get-by-email {:email email})]
    (if (not (nil? user))
      {:error "Email is alread in use"}
      (let [hashed-password (auth/hash-password password)
            user (create-user {:username username
                               :email email
                               :password hashed-password
                               :created-at (str (java.util.Date.))})]
        (if (nil? user)
          {:error "Couldn't create user with that email and password"}
          {:user user})))))

(defact ->login [{:keys [get-by-email]}]
  {:pre [(fn? get-by-email)]}
  [{:keys [email password]}]
  (let [user (get-by-email {:email email})]
    (when (and user (auth/verify-password password (:password user)))
      user)))

(defact ->get-user [_] [])
(defact ->get-id-from-username [_] [])
(defact ->get-profile [_] [])
(defact ->get-following [_] [])
(defact ->follow [_] [])
(defact ->unfollow [_] [])

(defmethod ig/init-key :core.services/user [_ {:keys [user-repo]}]
  {:register (->register user-repo)
   :login (->login user-repo)
   :get-user (->get-user user-repo)
   :get-id-from-username (->get-id-from-username user-repo)
   :get-profile (->get-profile user-repo)
   :get-following (->get-following user-repo)
   :follow (->follow user-repo)
   :unfollow (->unfollow user-repo)})
