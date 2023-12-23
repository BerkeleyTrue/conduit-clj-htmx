(ns conduit.app.drivers.auth
  (:require
    [conduit.infra.hiccup :refer [defhtml]]
    [conduit.infra.utils :as utils]
    [conduit.app.drivers.layout :refer [layout]]))

(defhtml render-auth [{:keys [isRegister]}]
    (layout
     [:div
      {:class "auth-page"}
      [:div
       {:class "container page"}
       [:div
        {:class "row"}
        [:div
         {:class "col-md-6 offset-md-3 col-xs-12"}
         [:h1 {:class "text-xs-center"} "Sign in"]
         [:p
          {:class "text-xs-center", :hx-boost "true"}
          (if isRegister
            [:a {:href "/login"} "Have an account?"]
            [:a {:href "/register"} "Need an account?"])]
         [:ul {:id "errors", :class "error-messages", :hidden ""}]
         [:form
          {:_ "on submit set { hidden: true } on #errors",
           :id "authen",
           :hx-post "/register",
           :hx-target "body",
           :hx-swap "outerHTML",
           :hx-push-url "true"}
          (when isRegister
            [:fieldset
             {:class "form-group"}
             [:input
              {:id "username",
               :name "username",
               :placeholder "Username",
               :class "form-control form-control-lg",
               :type "text"}]])
          [:fieldset
           {:class "form-group"}
           [:input
            {:id "email",
             :name "email",
             :placeholder "Email",
             :class "form-control form-control-lg",
             :type "text"}]]
          [:fieldset
           {:class "form-group"}
           [:input
            {:id "password",
             :name "password",
             :placeholder "Password",
             :class "form-control form-control-lg",
             :type "password"}]]
          [:button
           {:type "submit", :class "btn btn-lg btn-primary pull-xs-right"}
           "Sign in"]]]]]]))

(defn get-login-page [_]
  (utils/response
    (render-auth {:isRegister false})))

(defn get-register-page [_]
  (utils/response
    (render-auth {:isRegister true})))
