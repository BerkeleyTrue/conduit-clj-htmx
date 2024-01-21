(ns conduit.infra.middleware.auth
  (:require
   [buddy.auth.backends :as backends]
   [conduit.utils.dep-macro :refer [defact]]))

(def auth-backend (backends/session))

(defact ->authen-middleware
  "create an authorization middleware"
  [{:keys [get-by-id]}]
  [handler]
  (fn [request]
   (when-let [user-id (:identity request)]
     (let [user (get-by-id user-id)]
       (handler (assoc request :user user))))
   (handler request)))
