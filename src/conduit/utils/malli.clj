(ns conduit.utils.malli
  (:require
   [valip.predicates :as v]
   [malli.core :as m]
   [malli.registry :as mr]
   [malli.error :as me]
   [taoensso.timbre :as timbre]))

(def email
  [:and
   [:string]
   [:fn
    {:error/message "must be a valid email address"}
    v/email-address?]]) []

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

(def my-schema
  {:email email
   :password password})

(comment
  (letfn [(explain [schema value]
            (->
             schema
             (m/schema)
             (m/explain value)
             (me/humanize)))]
    (timbre/info (explain password "12a4B678"))
    (timbre/info (explain password "12345678"))
    (timbre/info (explain password "aA0"))
    (timbre/info (explain email "foobar"))))

(mr/set-default-registry!
 (mr/composite-registry
  (mr/schemas m/default-registry)
  my-schema))

(comment
  (m/schema
   [:map
    [:email :email]]))
