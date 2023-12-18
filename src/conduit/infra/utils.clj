(ns conduit.infra.utils
  (:require
   [ring.util.response :as response]))

(defn response [body]
  (->
    body
    (response/response)
    (response/content-type "text/html")))
