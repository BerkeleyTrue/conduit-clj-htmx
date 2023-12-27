(ns conduit.app.drivers.auth
  (:require
   [taoensso.timbre :as timbre]
   [conduit.infra.hiccup :refer [defhtml hyper]]
   [conduit.infra.utils :as utils]
   [conduit.app.drivers.layout :refer [layout]]))

(defhtml render-auth [{:keys [isRegister]}]
  (layout
   {:content
    [:div.auth-page
     [:div.container.page
      [:div.row
       [:div.col-md-6.offset-md-3.col-xs-12
        [:h1.text-xs-center
         (if isRegister
           "Sign up"
           "Sign in")]
        [:p.text-xs-center
         {:hx-boost "true"}
         (if isRegister
           [:a {:href "/login"} "Have an account?"]
           [:a {:href "/register"} "Need an account?"])]
        [:ul.error-messages {:id "errors" :hidden ""}]
        [:form
         (hyper
          "on submit set { hidden: true } on #errors"
          {:id "authen"
           :hx-post (if isRegister "/register" "/login")
           :hx-target "body"
           :hx-swap "outerHTML"
           :hx-push-url "true"})
         (when isRegister
           [:fieldset.form-group
            [:input.form-control.form-control-lg
             {:id "username"
              :name "username"
              :placeholder "Username"
              :type "text"}]])
         [:fieldset.form-group
          [:input.form-control.form-control-lg
           {:id "email"
            :name "email"
            :placeholder "Email"
            :type "text"}]]
         [:fieldset.form-group
          [:input.form-control.form-control-lg
           {:id "password"
            :name "password"
            :placeholder "Password"
            :type "password"}]]
         [:button.btn.btn-lg.btn-primary.pull-xs-right
          {:type "submit"}
          "Sign in"]]]]]]}))

(defn get-login-page [_]
  (utils/response
   (render-auth {:isRegister false})))

(defn get-register-page [_]
  (utils/response
   (render-auth {:isRegister true})))

(defn post-login-page [request]
  (let [params (:params request)]
    (timbre/info "params" params)
    (utils/response
     (render-auth {:isRegister false}))))
