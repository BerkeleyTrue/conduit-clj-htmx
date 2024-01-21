(ns conduit.env
  (:require
   [malli.dev :as dev]
   [conduit.dev-middleware :refer [dev-middleware]]
   [conduit.timbre]))

(def defaults
  {:middleware dev-middleware
   :dev-routes []
   :config
   {:profile :dev}})

(dev/start!)
