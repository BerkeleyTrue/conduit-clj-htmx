(ns conduit.env
  (:require
   [malli.dev :as dev]
   [malli.dev.pretty :as pretty]))

(def defaults
  {:middleware []
   :dev-routes []
   :config
   {:profile :dev}})

(dev/start! {:report (pretty/reporter)})
