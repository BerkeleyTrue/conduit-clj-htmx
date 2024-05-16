(ns conduit.core.services.user
  (:require
   [java-time.api :as jt]
   [integrant.core :as ig]
   [malli.core :as m]
   [conduit.utils.auth :as auth]
   [conduit.core.models :refer [User]]
   [conduit.core.ports.user-repo :as repo])
  (:import
   [java.util UUID Date]))

(def default-image "https://static.productionready.io/images/smiley-cyrus.jpg")

(def UserProfile
  [:map
   [:username :string]
   [:bio {:optional true} [:maybe :string]]
   [:image {:optional true} [:maybe :string]]
   [:following? :boolean]])

(m/=> format-to-public-profile [:=> [:cat User :boolean] UserProfile])
(defn format-to-public-profile [user following?]
  (let [image (or (:image user) default-image)]
    {:username (:username user)
     :bio (:bio user)
     :image image
     :following? following?}))

(defprotocol UserService
  "Users API"
  (register [_ params] "Register a new user")
  (login [_ params] "A user attempts to login")
  (find-user [_ params] "Find a user by id, username, or email")
  (get-profile [_ {:keys [user-id author-id authorname]}] "Get a authors profile by id or username")
  (get-following [_ user-id] "Get a users follows")
  (update-user [_ params] "Update a user")
  (follow-author [_ user-id {:keys [author-id authorname]}] "A user follows an author")
  (unfollow-author [_ user-id {:keys [author-id authorname]}] "A user unfollows an author"))

(defn service? [user-service?]
  (satisfies? UserService user-service?))

(defmethod ig/init-key :core.services/user [_ {:keys [repo]}]
  (assert (repo/repo? repo) (str "User service expects a user repository but found: " repo))
  (reify UserService
    (register [_ {:keys [username email password]}]
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
                       :created-at (jt/instant)})]
            (if (nil? user)
              [:error "Could not create user with that email and password"]
              [:ok user])))))

    (login [_ {:keys [email password]}]
      (let [user (repo/get-by-email repo email)]
        (if (and user (auth/verify-password password (:password user)))
          [:ok user]
          [:error "No user with that email and password was found"])))

    (find-user [_ {:keys [user-id username email]}]
      (let [user (cond
                   user-id (repo/get-by-id repo user-id)
                   username (repo/get-by-username repo username)
                   email (repo/get-by-email repo email))]
        (if (nil? user)
          [:error "No user found"]
          [:ok user])))

    (get-profile [_ {:keys [user-id author-id authorname]}]
      (let [author (if author-id
                     (repo/get-by-id repo author-id)
                     (repo/get-by-username repo authorname))
            user (repo/get-by-id repo user-id)]
        (if (nil? author)
          [:error "No user found"]
          (let [following? (if user
                             (contains? (:following user) (:user-id author))
                             false)]
            [:ok (format-to-public-profile author following?)]))))

    (get-following [_ {:keys [user-id]}]
      (if-let [following (repo/get-following repo user-id)]
        [:ok following]
        [:error (str "Could not find a following for " user-id)]))

    (update-user [_ {:keys [user-id] :as params}]
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

    (follow-author [_ user-id {:keys [author-id authorname]}]
      (if-not (or author-id authorname)
        [:error "follow requires author name or id"]
        (if-let [author-id (if author-id
                             author-id
                             (:user-id (repo/get-by-username repo authorname)))]
          (if-let [author (repo/follow-author repo user-id author-id)]
            [:ok (format-to-public-profile author true)]
            [:error (str "Could not find a user for user-id " user-id)])
          [:error (str "Could not find author for authorname " authorname)])))

    (unfollow-author [_ user-id {:keys [author-id authorname]}]
      (if-not (or author-id authorname)
        [:error "follow requires author name or id"]
        (if-let [author-id (if author-id
                             author-id
                             (:user-id (repo/get-by-username repo authorname)))]
          (if-let [author (repo/unfollow-author repo user-id author-id)]
            [:ok (format-to-public-profile author false)]
            [:error (str "Could not find a user for user-id " user-id)])
          [:error (str "Could not find author for authorname " authorname)])))))
