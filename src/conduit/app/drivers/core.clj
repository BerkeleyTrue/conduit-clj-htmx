(ns conduit.app.drivers.core
  (:require
    [integrant.core :as ig]
    [conduit.app.drivers.hot-reload :as hot-reload]
    [conduit.app.drivers.home :as home]
    [conduit.app.drivers.auth :as auth]
    [conduit.app.drivers.layout :refer [render-middleware]]
    [conduit.app.drivers.settings :as settings]
    [conduit.app.drivers.profile :as profile]
    [conduit.app.drivers.articles :as articles]
    [conduit.app.drivers.tag :as tag]))

(defmethod ig/init-key :app.routes/drivers
  [_ {:keys [on-start-ch user-service article-service]}]
  [["/__hotreload" {:name :hotreload
                    :get (hot-reload/->get-sse on-start-ch)}]
   (into
     ["/" {:middleware [render-middleware]}]
     [(home/->home-routes)
      (auth/->auth-routes user-service)
      (settings/->settings-routes user-service)
      (profile/->profile-routes user-service)
      (articles/->articles-routes article-service)
      (tag/->tag-routes article-service)])])

(derive :app.routes/drivers :app/routes)
