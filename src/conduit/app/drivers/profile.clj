(ns conduit.app.drivers.profile
  (:require
   [clojure.core.match :refer [match]]
   [ring.util.response :as response]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.utils.hyper :refer [hyper]]
   [conduit.infra.utils :as utils]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.infra.middleware.flash :refer [push-flash]]
   [conduit.core.services.user :refer [service? find-user follow-author unfollow-author get-following]]))

(def place-holder "https://static.productionready.io/images/smiley-cyrus.jpg")

(defhtml profile-follow-button [authorname num-of-followers following?]
  [:button#profile-follow-btn.btn.btn-sm.btn-outline-secondary.follow-btn
   (assoc {:hx-swap "outerHTML"} (if following? :hx-delete :hx-post) (str "/profiles/" authorname "/follow"))
   [:i.ion-plus-round
    (str " " (if following? "Unfollow " "Follow ") authorname)
    [:span.counter (str " (" num-of-followers ")")]]])

(defhtml profile-component [{:keys [username image bio self? following? num-of-followers]}]
  [:div.profile-page
   [:div.user-info
    [:div.container
     [:div.row
      [:div.col-xs-12.col-md-10.offset-md-1
       [:img {:src (if (empty? image) place-holder image) :class "user-img"}]
       [:h4 {:class "user-name"} username]
       [:p (if (seq bio) bio "Go to settings to add a bio!")]
       (if self?
         [:button.btn.btn-sm.btn-outline-secondary.action-btn
          {:hx-get "/settings"
           :hx-target "body"
           :hx-swap "innerHTML"
           :hx-push-url "/settings"}
          [:i.ion-gear-a " Edit Profile Settings"]]
         (profile-follow-button username num-of-followers following?))]]]]

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
          :hx-get (str "/articles?author=" username)
          :hx-trigger "click, load delay:150ms" 
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
          :hx-get (str "/articles?favorited=" username)
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
  [user-service]
  {:pre [(service? user-service)]}
  [request]
  (let [user-id (get request :user-id)
        username (get-in request [:path-params :username])
        self? (= (:username request) username)
        res (if self?
              [:ok (:user request)]
              (find-user user-service {:username username}))]
    (match res
      [:error _error] (-> (response/redirect "/" :see-other)
                          (push-flash :warning (str "No user found for " username)))
      [:ok user] (let [followers (match (get-following user-service {:username username})
                                    [:ok following] following
                                    _ 0)]
                   {:render {:title (str "Profile: " username)
                             :content (profile-component 
                                        (assoc user
                                               :self? self?
                                               :following? (if user-id
                                                             (contains? followers user-id)
                                                             false)
                                               :num-of-followers (count followers)))}}))))

(defn ->follow-author [user-service]
  (fn [request]
    (let [authorname (get-in request [:parameters :path :username])
          user-id (get request :user-id)
          update-profile? (= "profile-follow-btn" (get-in request [:headers "hx-trigger"]))]
      (match (follow-author user-service user-id {:authorname authorname})
        [:error error] (utils/list-errors {:user error})
        [:ok _profile] (if update-profile?
                         (match (get-following user-service {:username authorname})
                           [:error error] 
                           (utils/list-errors {:user error})

                           [:ok followers] 
                           (-> (profile-follow-button authorname (count followers) true)
                               (utils/response)))
                         (response/status 200))))))

(defn ->unfollow-author [user-service]
  (fn [request]
    (let [authorname (get-in request [:parameters :path :username])
          user-id (get request :user-id)
          update-profile? (= "profile-follow-btn" (get-in request [:headers "hx-trigger"]))]
      (match (unfollow-author user-service user-id {:authorname authorname})
        [:error error] (utils/list-errors error)
        [:ok _profile] (if update-profile?
                         (match (get-following user-service authorname)
                           [:error error] 
                           (utils/list-errors {:user error})

                           [:ok followers] 
                           (-> (profile-follow-button authorname (count followers) false)
                               (utils/response)))

                         (response/status 200))))))

(defn ->profile-routes [user-service]
  ["profiles"
   ["/:username" {:name :profiles
                  :parameters {:path {:username :string}}}
    ["" {:name :profile/get
         :get {:handler (->get-profile-page user-service)}}]
    ["/follow" {:name :profile/follow
                :middleware [:authorize]
                :post {:handler (->follow-author user-service)}
                :delete {:handler (->unfollow-author user-service)}}]]])
