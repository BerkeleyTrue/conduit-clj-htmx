(ns conduit.dev-middleware
  (:require
   [ring.middleware.stacktrace :refer [wrap-stacktrace]]
   [ring.middleware.reload :refer [wrap-reload]]))

(def dev-middleware
  [{:name ::dev-stacktrace
    :wrap wrap-stacktrace}
   {:name ::dev-reload
    :wrap wrap-reload}])
