(ns conduit.infra.middleware.auth
  (:require
   [buddy.auth.backends :as backends]
   [ring.util.response :as response]
   [taoensso.timbre :as timbre]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.core.services.user :refer [service? find-user]]))

(def auth-backend (backends/session))

(defact ->authen-middleware
  "create an authentication middleware, which pulls user
  data of the sesion and injects it into the request"
  [user-service]
  {:pre [(service? user-service)]}
  [handler]
  (fn authen-middleware [request]
    (if-let [user-id (:identity request)]
      (let [res (find-user user-service {:user-id user-id})]
        (timbre/info "User session: " user-id)
        (if-let [user (:user res)]
          (handler (->
                    request
                    (assoc :user user)
                    (assoc :user-id user-id)
                    (assoc :username (:username user))))
          (->
           (response/redirect "/login" :see-other)
           (update :session dissoc :identity))))
      (handler request))))

(defn authorize-middleware
  "An authorization middleware, which checks if the
  user is authorized to access the resource"
  [handler]
  (fn authorize-handler
    [request]
    (if-not (nil? (:user-id request))
      (handler request)
      (response/redirect "/login" :see-other))))
