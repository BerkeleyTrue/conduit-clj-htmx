(ns conduit.infra.utils
  (:require
   [ring.util.response :as response]
   [conduit.infra.hiccup :refer []]))

(defn response [hiccup-str]
  (->
    hiccup-str
    (str)
    (response/response)
    (response/content-type "text/html")))
