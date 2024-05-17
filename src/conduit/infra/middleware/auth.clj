(ns conduit.infra.middleware.auth
  (:require
   [clojure.core.match :refer [match]]
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
      (match (find-user user-service {:user-id user-id})
        [:ok user] (do
                     (timbre/info "User session: " user-id)
                     (handler (-> request
                                  (assoc :user user)
                                  (assoc :user-id user-id)
                                  (assoc :username (:username user)))))
        [:error error] (do 
                         (timbre/info "User session: " error) 
                         (if (get-in request [:headers "Hx-Request"])
                           (-> (response/redirect "/login" 200)
                               (response/header "HX-Redirect" "/login")
                               (update :session dissoc :identity))
                           (-> (response/redirect "/login" :see-other)
                               (update :session dissoc :identity)))))
      (handler request))))

(defn authorize-middleware
  "An authorization middleware, which checks if the
  user is authorized to access the resource"
  [handler]
  (fn authorize-handler
    [request]
    (if-not (nil? (:user-id request))
      (handler request)
      (if (get-in request [:headers "Hx-Request"])
        (-> (response/redirect "/login" 200) ; HTMX request expects a 200 in order to process hx-redirect
            (response/header "HX-Redirect" "/login"))
        (response/redirect "/login" :see-other)))))
