(ns conduit.env
  (:require
   [conduit.dev-middleware :refer [dev-middleware]]
   [conduit.timbre]))

(def defaults
  {:middleware dev-middleware
   :dev-routes []
   :config
   {:profile :dev}})
