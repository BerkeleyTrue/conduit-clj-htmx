(ns conduit.app.drivers.home
  (:require
   [conduit.infra.utils :as utils]))

(defn get-home-page
  "Returns the home page."
  [_]
  (utils/response-hiccup [:h1 "Welcome to Conduit!"]))
