(ns conduit.utils.auth
  (:require
   [buddy.hashers :as hashers]))

(def alg :bcrypt+blake2b-512)

(defn hash-password [password]
  (hashers/derive password {:alg alg}))

(defn verify-password [password hash]
  (hashers/verify password hash))

(comment
  (hash-password "password1234")
  (verify-password "password1234" (hash-password "password1234"))
  (verify-password "password1234" (hash-password "password12345"))
  ,)
