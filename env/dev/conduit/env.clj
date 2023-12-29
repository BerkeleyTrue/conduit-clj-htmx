(ns conduit.env
  (:require
   [conduit.dev-middleware :refer [dev-middleware]]))

(def defaults
  {:middleware dev-middleware
   :dev-routes []
   :config
   {:profile :dev}})
