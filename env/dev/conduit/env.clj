(ns conduit.env
  (:require
   [conduit.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:middleware wrap-dev
   :dev-routes []})
