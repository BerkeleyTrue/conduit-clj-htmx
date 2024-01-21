(ns conduit.app.drivers.core
  (:require
    [integrant.core :as ig]
    [conduit.app.drivers.home :as home]
    [conduit.app.drivers.auth :as auth]
    [conduit.app.drivers.hot-reload :as hot-reload]))

(def authed-links
  [{:uri "/"
    :title "Home"}
   {:uri "/editor"
    :title "New Article"}
   {:uri "/settings"
    :title "Settings"}])

(def unauthed-links
  [{:uri "/"
    :title "Home"}
   {:uri "/login"
    :title "Sign in"}
   {:uri "/register"
    :title "Sign up"}])

(defn app-middleware
  "add app data to request, such as layout props"
  [handler]
  (fn [request]
    (let [links (if (:user request) authed-links unauthed-links)]
      (handler (->
                 request
                 (assoc
                   :layout-props
                   {:title "Conduit"
                    :page (:page request)
                    :uri (:uri request)
                    :user (:user request)
                    :user-id (:user-id request)
                    :username (:username request)
                    :links links}))))))

(defmethod ig/init-key :app.routes/drivers
  [_ {:keys [on-start-ch user-service]}]
  ["/" {:middleware [app-middleware]}
   ["" {:name :get-home
        :get home/get-home-page}]
   ["__hotreload" {:name :hotreload
                   :get (hot-reload/->get-sse on-start-ch)}]
   (auth/->login-routes user-service)
   (auth/->register-routes user-service)])

(derive :app.routes/drivers :app/routes)
