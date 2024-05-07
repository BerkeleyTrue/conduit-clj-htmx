(ns conduit.app.drivers.profile
  (:require
   [clojure.core.match :refer [match]]
   [ring.util.response :as response]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.infra.middleware.flash :refer [push-flash]]
   [conduit.utils.hyper :refer [hyper]]))

(def place-holder "https://static.productionready.io/images/smiley-cyrus.jpg")

(defhtml follow-button-component [{:keys [username following?]}]
  [:button#profile-follow-button.btn.btn-sm.btn-outline-secondary.follow-btn
   (-> {:hx-swap "outerHtml"}
       (assoc (if following? :hx-delete :hx-post) (str "/profiles/" username "/follow")))
   [:i.ion-plus-round
    (str " " (if following? "Unfollow" "Follow") " " username)
    [:span.counter "(?)"]]])

(defhtml profile-component [{:keys [username image bio self? authed?]}]
  [:div.profile-page
   [:div.user-info
    [:div.container
     [:div.row
      [:div.col-xs-12.col-md-10.offset-md-1
       [:img {:src (if (empty? image) place-holder image) :class "user-img"}]
       [:h4 {:class "user-name"} username]
       [:p (if (seq bio) bio "Go to settings to add a bio!")]
       (when authed?
         (if self?
           [:button.btn.btn-sm.btn-outline-secondary.action-btn
            {:hx-get "/settings"
             :hx-target "body"
             :hx-swap "innerHTML"
             :hx-push-url "/settings"}
            [:i.ion-gear-a " Edit Profile Settings"]]
           (follow-button-component {:username username
                                     ; TODO: implement backend
                                     :following? false})))]]]]
   [:div.container
    [:div.row
     [:div.col-xs-12.col-md-10.offset-md-1
      [:div.articles-toggle
       [:ul.nav.nav-pills.outline-active
        {:role "tablist"}
        [:li.nav-item
         {:_ (hyper
              "
                on click
                  set innerHTML of #articles to 'Loading articles...'
                  remove .active from .nav-link
                  add .active to .nav-link in me
                ")
          :role "tab"
          :hx-get (str "/profiles/" username "/articles")
          ; :hx-trigger "click, load delay:150ms" ; TODO: implement backend
          :hx-target "#articles"}
         [:a.nav-link.active
          "My Articles"]]
        [:li.nav-item
         {:_ (hyper
              "
                on click
                  set innerHTML of #articles to 'Loading articles...'
                  remove .active from .nav-link
                  add .active to .nav-link in me
                ")
          :hx-get (str "/articles?author=" username)
          :hx-target "#articles"}
         [:a.nav-link
          "Favorited Articles"]]]]
      [:div#articles
       [:div.article-preview
        "Loading articles..."]]
      [:ul#pagination.pagination
       ; TODO: implement backend
       {:hidden true}]]]]])

(defact ->get-profile-page
  [{:keys [find-user]}]
  {:pre [(fn? find-user)]}
  [request]
  (let [username (get-in request [:path-params :username])
        self? (= (:username request) username)
        res (if self?
              [:ok (:user request)]
              (find-user {:username username}))]
    (match res
      [:error _error] (-> (response/redirect "/")
                          (push-flash :warning (str "No user found for " username)))
      [:ok user] {:render {:title (str "Profile: " username)
                           :content (profile-component (assoc user
                                                              :self? self?
                                                              :authed? (not (nil? (:user-id request)))))}})))

(defn ->profile-routes [user-service]
  ["profiles/:username"
   {:get (->get-profile-page user-service)
    :parameters {:path {:username :string}}}])
