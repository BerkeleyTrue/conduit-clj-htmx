(ns conduit.infra.malli
  (:require
   [valip.predicates :as v]
   [malli.core :as m]
   [malli.registry :as mr]
   [malli.error :as me]))

(def email
  [:and
   [:string]
   [:fn
    {:error/message "must be a valid email address"}
    v/email-address?]]) 

(def password
  [:and
   [:string
    {:min 8
     :error/message "must be a string with at least 8 characters"}]
   [:re
    {:error/message "must contain at least one uppercase letter"}
    #"[A-Z]"]
   [:re
    {:error/message "must contain at least one lowercase letter"}
    #"[a-z]"]
   [:re
    {:error/message "must contain at least one digit"}
    #"[0-9]"]])

(def _empty
  [:fn
   {:error/message "must be empty"}
   empty?])

(def xt-trans
  [:qualified-keyword {:namespace :xtdb.api
                       :error/message "Must be a xtdb.api transaction method"}])

(def my-schema
  {:email email
   :password password
   :empty _empty
   :instant inst?
   :xt-trans xt-trans})

(comment
  (letfn [(explain [schema value]
            (->
             schema
             (m/schema)
             (m/explain value)
             (me/humanize)))]
    (println (explain xt-trans :api/delete))
    (println (explain password "12a4B678"))
    (println (explain password "12345678"))
    (println (explain password "aA0"))
    (println (explain email "foobar"))
    (println (explain _empty ""))
    (println (explain [:or _empty :password] ""))))

(mr/set-default-registry!
 (mr/composite-registry
  (mr/schemas m/default-registry)
  my-schema))

(comment
  (m/schema
   [:map
    [:email :email]]))
