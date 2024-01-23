(ns conduit.infra.middleware.auth
  (:require
   [buddy.auth.backends :as backends]
   [conduit.utils.dep-macro :refer [defact]]
   [ring.util.response :as response]
   [taoensso.timbre :as timbre]))

(def auth-backend (backends/session))

(defact ->authen-middleware
  "create an authorization middleware"
  [{:keys [get-by-id]}]
  {:pre [(fn? get-by-id)]}
  [handler]
  (fn [request]
    (when-let [user-id (:identity request)]
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
    (handler request)))
