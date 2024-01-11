(ns conduit.infra.utils
  (:require
   [ring.util.response :as response]
   [conduit.infra.hiccup :refer [defhtml]]))

(defn response
  "Send a hiccup object as a response
  with content type set to html and status 200"
  [hiccup-obj]
  (->
    hiccup-obj
    (str)
    (response/response)
    (response/content-type "text/html")))

(defhtml list-errors
  "Render a list of errors in a ul"
  [errsm]
  [:div#errors.error-messages
   {:hx-swap-oob :true} ; must be a string
   (for [ky (keys errsm)
         :let [errs (errsm ky)
               errs (if (string? errs) [errs] errs)]]
     (for [err errs]
        [:li
         (str (name ky) ": " err)]))])

(defn list-errors-response
  "Send a list of errors as a html 200 response,
  push url is set to false and reswap is set to none"
  [errsm]
  (->
    (list-errors errsm)
    (response)
    (response/header "HX-Push-URL" false)
    (response/header "HX-Reswap" "none")))

(comment
  (list-errors {"email" "is invalid"
                "password" "can't be blank"})
  (list-errors {:email "is invalid"
                :password "can't be blank"})
  (list-errors {:email ["is invalid" "is too short (minimum is 1 character)"]
                :password "can't be blank"})
  (list-errors-response {"email" "is invalid"
                         "password" "can't be blank"}))
