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
   [:bio {:optional true} [:maybe :string]]
   [:image {:optional true} [:maybe :string]]
   [:following [:set :uuid]] ; user ids

   [:created-at :instant]
   [:updated-at {:optional true } [:maybe :instant]]])

(def Article
  [:map
   {:title "Article"
    :description "An article"}
   [:article-id :uuid]
   [:author-id :uuid]
   [:title :string]
   [:slug :string]
   [:description :string]
   [:body :string]
   [:tags [:set :string]]

   [:created-at :instant]
   [:updated-at {:optional true} [:maybe :instant]]])

(def Comment
  [:map
   {:title "Comment"
    :description "A comment on an article"}
   [:comment-id :uuid]
   [:article-id :uuid]
   [:author-id  :uuid]
   [:body :string]

   [:created-at :instant]])

(comment
  (m/schema User)
  (m/schema Article)
  (m/schema Comment))
