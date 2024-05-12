(ns conduit.app.drivers.settings
  (:require
   [clojure.core.match :refer [match]]
   [ring.util.response :as response]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.utils.hyper :refer [hyper]]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.infra.utils :as utils]
   [conduit.infra.middleware.flash :refer [push-flash]]
   [conduit.core.services.user :refer [service? update-user]]))

(defhtml settings-component [{:keys [username image email bio]}]
  [:div.settings-page
   [:div.container.page
    [:div.row
     [:div.col-md-6.offset-md-3.col-xs-12
      [:h1.text-xs-center "Your Settings"]
      [:ul.error-messages {:id "errors" :hidden "true"}]
      [:form
       {:id "settings"
        :hx-post "/settings"
        :hx-target "#settings"
        :hx-select "#settings"
        :hx-swap "outerHTML"
        :_ (hyper "on submit set { hidden: true } on #errors")}
       [:fieldset
        [:fieldset.form-group
         [:input.form-control
          {:id "image"
           :name "image"
           :placeholder "URL of profile picture"
           :value image
           :type "text"}]]
        [:fieldset.form-group
         [:input.form-control.form-control-lg
          {:id "username"
           :name "username"
           :placeholder "Your Name"
           :value username
           :type "text"}]]
        [:fieldset.form-group
         [:textarea.form-control.form-control-lg
          {:id "bio"
           :name "bio"
           :placeholder "Short bio about you"
           :rows "8"}
          (when bio bio)]]
        [:fieldset.form-group
         [:input.form-control.form-control-lg
          {:id "email"
           :name "email"
           :placeholder "Email"
           :value email
           :type "email"}]]
        [:fieldset.form-group
         [:input.form-control.form-control-lg
          {:id "password"
           :name "password"
           :placeholder "New Password"
           :type "password"}]]
        [:button.btn.btn-lg.btn-primary.pull-xs-right
         {:type "submit"}
         "Update Settings"]]]
      [:hr]
      [:button.btn.btn-outline-danger
       {:hx-post "/logout"
        :hx-target "body"
        :hx-swap "outerHTML"}
       "Or click here to logout."]]]]])

(defn get-settings-page [request]
  (if-let [user (:user request)]
    {:render {:title "Settings"
              :content (settings-component user)}}
    (response/redirect "/login")))

(defact ->post-settings-page
  [user-service]
  {:pre [(service? user-service)]}
  [request]
  (let [params (:params request)
        password? (seq (:password params))
        user-id (:user-id request)]

    (match (update-user user-service (assoc params :user-id user-id))
      [:error error] (utils/list-errors-response {:settings error})
      [:ok user] (-> {:render {:title "Settings"
                               :content (settings-component user)}}
                     (push-flash :success (if password? "Settings updated!" "Password updated!"))
                     (update :session assoc :identity (:user-id user))))))

(defn ->settings-routes [user-service]
  ["settings"
   {:middleware [:authorize]
    :get {:name :settings/get
          :handler get-settings-page}
    :post
    {:name :settins/update
     :handler (->post-settings-page user-service)
     :parameters
     {:form
      [:map
       {:closed true}
       [:email :email]
       [:username :string]
       [:image :string]
       [:bio :string]
       [:password [:or :empty :password]]]}}}])
