(ns conduit.core.models.user
  (:require
   [malli.core :as m]))

(def User
  [:map
   {:title "User"
    :description "A user"}
   [:user/id number?]
   [:username string?]
   [:email string?]
   [:password string?]
   [:bio string?]
   [:image string?]
   [:followers [:set number?]] ; user ids

   [:created-at string?]
   [:updated-at string?]])

(def Article
  [:map
   {:title "Article"
    :description "An article"}
   [:articleId number?]
   [:authorId number?]
   [:title string?]
   [:slug string?]
   [:description string?]
   [:body string?]
   [:tags [:set string?]]

   [:created-at string?]])

(def Comment
  [:map
   {:title "Comment"
    :description "A comment"}
   [:comment-id number?]
   [:article-id number?]
   [:author-id number?]
   [:body string?]

   [:created-at string?]])

(comment
  (m/schema User)
  (m/schema Article)
  (m/schema Comment))
