(ns conduit.infra.middleware.auth
  (:require
   [buddy.auth.backends :as backends]))

(def auth-backend (backends/session))
