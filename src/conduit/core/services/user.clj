(ns conduit.core.services.user
  (:require
   [conduit.utils.auth :as auth]
   [integrant.core :as ig]
   [malli.core :as m]
   [conduit.core.models :refer [User]]
   [conduit.core.ports.user-repo :as repo]
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

(m/=> format-to-public-profile [:=> [:cat User :boolean] User-Profile])
(defn format-to-public-profile [user following?]
  (let [image (or (:image user) default-image)]
    {:username (:username user)
     :bio (:bio user)
     :image image
     :following? following?}))

(defact ->register [repo]
  [{:keys [username email password]}]
  (let [user (repo/get-by-email repo email)]
    (if-not (nil? user)
      [:error "Email is alread in use"]
      (let [hashed-password (auth/hash-password password)
            user (repo/create
                   repo
                   {:user-id (UUID/randomUUID)
                    :username username
                    :email email
                    :password hashed-password
                    :created-at (str (Date.))})]
        (if (nil? user)
          [:error "Could not create user with that email and password"]
          [:ok user])))))

(defact ->login [repo]
  [{:keys [email password]}]
  (let [user (repo/get-by-email repo email)]
    (if (and user (auth/verify-password password (:password user)))
      [:ok user]
      [:error "No user with that email and password was found"])))

(defact ->find-user [repo]
  [{:keys [user-id username email]}]
  (let [user (cond
               user-id (repo/get-by-id repo user-id)
               username (repo/get-by-username repo username)
               email (repo/get-by-email repo email))]
    (if (nil? user)
      [:error "No user found"]
      [:ok user])))

(defact ->get-profile [repo] 
  [{:keys [author-id authorname user-id]}]
  (let [author (if author-id
                 (repo/get-by-id repo author-id)
                 (repo/get-by-username repo authorname))]
    (if (nil? author)
      [:error "No user found"]
      (let [following? (if user-id
                         (contains? (:following author) user-id)
                         false)]
        [:ok (format-to-public-profile author following?)]))))

(defact ->get-following [repo]
  [{:keys [user-id]}]
  (if-let [following (repo/get-following repo user-id)]
    [:ok following]
    [:error (str "Could not find a following for " user-id)]))

(defact ->update
  [repo]
  [{:keys [user-id] :as params}]
  (if (nil? user-id)
    [:error "User id or username is required"]
    (let [now (str (Date.))
          params (if (empty? (:password params))
                   (assoc params :password (auth/hash-password (:password params)))
                   (dissoc params :password))
          user (repo/update repo user-id (assoc params :updated-at now))]
      (if (nil? user)
        [:error "Couldn't update user"]
        [:ok user]))))

(defact ->follow [repo] 
  [{:keys [user-id author-id authorname]}]
  (if-not (or author-id authorname)
    [:error "follow requires author name or id"]
    (if-let [author-id (if author-id 
                         author-id
                         (:user-id (repo/get-by-username repo authorname)))]
      (if-let [user (follow {:user-id user-id 
                             :author-id author-id})]
        [:ok (format-to-public-profile user true)]
        [:error (str "Could not find a user for user-id " user-id)])
      [:error (str "Could not find author for authorname " authorname)])))

(defact ->unfollow [repo] 
  [{:keys [user-id author-id authorname]}]
  (if-not (or author-id authorname)
    [:error "follow requires author name or id"]
    (if-let [author-id (if author-id 
                         author-id
                         (:user-id (repo/get-by-username repo authorname)))]
      (if-let [user (unfollow {:user-id user-id 
                               :author-id author-id})]
        [:ok (format-to-public-profile user true)]
        [:error (str "Could not find a user for user-id " user-id)])
      [:error (str "Could not find author for authorname " authorname)])))

(defmethod ig/init-key :core.services/user [_ {:keys [repo]}]
  (assert (repo/repo? repo) (str "User service expects a user repository but found: " repo))
  {:register (->register repo)
   :login (->login repo)
   :find-user (->find-user repo)
   :get-profile (->get-profile repo)
   :get-following (->get-following repo)
   :follow (->follow repo)
   :unfollow (->unfollow repo)
   :update (->update repo)})
