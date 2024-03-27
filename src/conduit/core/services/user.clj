(ns conduit.core.services.user
  (:require
   [conduit.utils.auth :as auth]
   [integrant.core :as ig]
   [malli.core :as m]
   [conduit.core.models :refer [User]]
   [conduit.utils.dep-macro :refer [defact]])
  (:import
   [java.util UUID Date]))

(def default-image "https://static.productionready.io/images/smiley-cyrus.jpg")

(def User-Profile
  [:map
   [:username :string]
   [:bio {:optional true} [:maybe :string]]
   [:image {:optional true} [:maybe :string]]
   [:following? :boolean]])

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

(m/=> format-to-public-profile [:=> [:cat User :boolean] [User-Profile]])
(defn format-to-public-profile [user following?]
  (let [image (or (:image user) default-image)]
    {:username (:username user)
     :bio (:bio user)
     :image image
     :following? following?}))

(defact ->get-profile 
  [{:keys [get-by-id get-by-username]}] 
  {:pre [(fn? get-by-id)]}
  [{:keys [author-id authorname user-id]}]
  (let [author (if author-id
                 (get-by-id author-id)
                 (get-by-username authorname))]
    (if (nil? author)
      {:error "No user found"}
      (let [following? (if user-id
                         (contains? (:following author) user-id)
                         false)]
        (format-to-public-profile author following?)))))

(defact ->get-following [{:keys [get-following]}]
  {:pre [(fn? get-following)]}
  [{:keys [user-id]}]
  (get-following user-id))

(defact ->follow [{:keys [follow get-id-from-username]}] 
  {:pre [(fn? follow) (fn? get-id-from-username)]}
  [{:keys [user-id author-id authorname]}]
  (let [author-id (if author-id 
                    author-id
                    (get-id-from-username authorname))
        user (follow {:user-id user-id 
                      :author-id author-id})]
    (format-to-public-profile user true)))

(defact ->unfollow [{:keys [unfollow get-id-from-username]}] 
  {:pre [(fn? unfollow) (fn? get-id-from-username)]}
  [{:keys [user-id author-id authorname]}]
  (let [author-id (if author-id 
                    author-id
                    (get-id-from-username authorname))
        user (unfollow {:user-id user-id 
                        :author-id author-id})]
    (format-to-public-profile user false)))

(defmethod ig/init-key :core.services/user [_ {:keys [user-repo]}]
  {:register (->register user-repo)
   :login (->login user-repo)
   :find-user (->find-user user-repo)
   :get-profile (->get-profile user-repo)
   :get-following (->get-following user-repo)
   :follow (->follow user-repo)
   :unfollow (->unfollow user-repo)
   :update (->update user-repo)})
