(ns conduit.core.models.user
  (:require
    [schema.core :as s]))

(s/defschema User
  {:user-id s/Int
   :username s/Str
   :email s/Str
   :password s/Str ; TODO: use a better type
   :bio s/Str
   :image s/Str
   :followers #{s/Int} ; user ids

   :created-at s/Str
   :updated-at s/Str})

(s/defschema Article
  {:articleId s/Int
   :authorId s/Int
   :title s/Str
   :slug s/Str
   :description s/Str
   :body s/Str
   :tags #{s/Str}

   :created-at s/Str})

(s/defschema Comment
  {:comment-id s/Int
   :article-id s/Int
   :author-id s/Int
   :body s/Str
   :created-at s/Str})
