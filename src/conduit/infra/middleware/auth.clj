(ns conduit.infra.middleware.auth
  (:require
   [buddy.auth.backends :as backends]
   [conduit.utils.dep-macro :refer [defact]]
   [ring.util.response :as response]
   [taoensso.timbre :as timbre]))

(def auth-backend (backends/session))

(defact ->authen-middleware
  "create an authentication middleware, which pulls user
  data of the sesion and injects it into the request"
  [{:keys [get-by-id]}]
  {:pre [(fn? get-by-id)]}
  [handler]
  (fn authen-middleware [request]
    (if-let [user-id (:identity request)]
      (do
        (timbre/info "User session: " user-id)
        (if-let [user (get-by-id user-id)]
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
