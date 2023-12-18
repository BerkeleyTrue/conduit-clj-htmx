(ns conduit.infra.utils
  (:require
   [hiccup2.core :as h]
   [ring.util.response :as response]))

(defn response-hiccup [hiccup]
  (->
    (h/html hiccup)
    (str)
    (response/response)
    (response/content-type "text/html")))
