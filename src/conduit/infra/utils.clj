(ns conduit.infra.utils
  (:require
   [ring.util.response :as response]
   [conduit.infra.hiccup :refer [defhtml]]))

(defn response [hiccup-str]
  (->
    hiccup-str
    (str)
    (response/response)
    (response/content-type "text/html")))

(defhtml list-errors [errsm]
  [:div#errors.error-messages
   {:hx-swap-oob :true} ; must be a string
   (for [ky (keys errsm)
         :let [errs (errsm ky)
               errs (if (string? errs) [errs] errs)]]
     (for [err errs]
        [:li
         (str (name ky) ": " err)]))])

(comment
  (list-errors {"email" "is invalid"
                "password" "can't be blank"})
  (list-errors {:email "is invalid"
                :password "can't be blank"})
  (list-errors {:email ["is invalid" "is too short (minimum is 1 character)"]
                :password "can't be blank"}))
