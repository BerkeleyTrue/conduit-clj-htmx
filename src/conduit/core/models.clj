(ns conduit.core.models
  (:require
   [malli.core :as m]))

(def User
  [:map
   {:title "User"
    :description "A user"}
   [:user-id :uuid]
   [:username :string]
   [:email :email]
   [:password :string]
   [:bio {:optional true} :string]
   [:image {:optional true} :string]
   [:following [:set :uuid]] ; user ids

   [:created-at :string]
   [:updated-at [:maybe :string]]])

(def Article
  [:map
   {:title "Article"
    :description "An article"}
   [:article-id :uuid]
   [:author-id :uuid]
   [:title string?]
   [:slug string?]
   [:description string?]
   [:body string?]
   [:tags [:set string?]]

   [:created-at string?]
   [:updated-at string?]])

(def Comment
  [:map
   {:title "Comment"
    :description "A comment on an article"}
   [:comment-id :uuid]
   [:article-id :uuid]
   [:author-id  :uuid]
   [:body string?]

   [:created-at string?]])

(comment
  (m/schema User)
  (m/schema Article)
  (m/schema Comment))
