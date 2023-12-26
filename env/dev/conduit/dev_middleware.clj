(ns conduit.dev-middleware
  (:require
   [ring.middleware.stacktrace :refer [wrap-stacktrace]]
   [ring.middleware.reload :refer [wrap-reload]]))

(defn wrap-dev [handler]
  (->
   handler
   (wrap-stacktrace)
   (wrap-reload)))
