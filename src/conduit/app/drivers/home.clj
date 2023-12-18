(ns conduit.app.drivers.home
  (:require
   [conduit.infra.utils :as utils]
   [conduit.app.drivers.layout :refer [layout]]))

(defn get-home-page
  "Returns the home page."
  [_]
  (utils/response (layout [:h1 "Welcome to Conduit!"])))
