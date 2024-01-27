(ns conduit.app.drivers.settings
  (:require
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.utils.hyper :refer [hyper]]
   [ring.util.response :as response]))

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

(defn ->settings-routes []
  ["settings" {:get get-settings-page}])
