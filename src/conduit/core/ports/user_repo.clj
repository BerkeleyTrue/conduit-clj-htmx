(ns conduit.core.ports.user-repo
  (:refer-clojure :exclude [list update]))

(defprotocol UserRepository
  (create [this params] "Create a user")
  (create-many [this users] "Create many users")
  (get-by-id [this user-id] "Find a user by id")
  (get-by-email [this email] "Find a user by email")
  (get-by-username [this username] "Find a user by their username")
  (get-following [this user-id] "Get the authors a user is following")
  (update [this user-id params] "Update key user data")
  (follow-author [this user-id author-id] "A user follows an author")
  (unfollow-author [this user-id author-id] "A user unfollows an author"))

(defn repo? [repo]
  (satisfies? UserRepository repo))
