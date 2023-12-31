(ns conduit.core.services.user
  (:require
   [conduit.utils.auth :as auth]
   [integrant.core :as ig]))

(defn ->register-service [{:keys [create-user]}]
  (fn register-service [{:keys [username email password]}]
    (let [hashed-password (auth/hash-password password)]
      (create-user
       {:username username
        :email email
        :password hashed-password
        :created-at (str (java.util.Date.))}))))

(defn ->login-service [repo-get-user]
  (fn login-service [{:keys [email password]}]
    (let [user (repo-get-user {:email email})]
      (when (and user (auth/verify-password password (:password user)))
        user))))

(defmethod ig/init-key :core.services/user [_ {:keys [user-repo]}]
  {:register (->register-service user-repo)
   :login (->login-service user-repo)})
