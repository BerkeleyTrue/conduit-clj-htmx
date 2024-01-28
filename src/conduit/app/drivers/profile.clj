(ns conduit.app.drivers.profile
  (:require
   [ring.util.response :as response]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.infra.middleware.flash :refer [push-flash]]))

(defhtml profile-component [{:keys [username image bio]}]
  [:div.profile-page
   [:div.user-info
    [:div.container
     [:div.row
      [:div.col-xs-12.col-md-10.offset-md-1
       [:img {:src image :class "user-img"}]
       [:h4 {:class "user-name"} username]
       [:p (when bio bio)]]]]]])

(defact ->get-profile-page
  [{:keys [find-user]}]
  {:pre [(fn? find-user)]}
  [request]
  (let [username (get-in request [:path-params :username])
        res (if (= (:username request) username)
              {:user (:user request)}
              (find-user {:username username}))]
    (if (nil? (:user res))
      (->
        (response/redirect "/")
        (push-flash :warning (str "No user found for " username)))
      {:render {:title (str "Profile: " username)
                :content (profile-component res)}})))

(defn ->profile-routes [user-service]
  ["profiles/:username"
   {:get (->get-profile-page user-service)
    :parameters {:path {:username :string}}}])
