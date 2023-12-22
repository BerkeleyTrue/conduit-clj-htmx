(ns conduit.core.models.user
  (:require
    [schema.core :as s]))

(s/defschema User
  {:userId s/Int
   :username s/Str
   :email s/Str
   :password s/Str
   :bio s/Str
   :image s/Str
   :createdAt s/Str
   :updatedAt s/Str
   :following s/Bool})
