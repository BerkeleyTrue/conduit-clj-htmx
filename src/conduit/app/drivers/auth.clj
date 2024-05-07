(ns conduit.app.drivers.auth
  (:require
   [clojure.core.match :refer [match]]
   [taoensso.timbre :as timbre]
   [ring.util.response :as response]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.infra.utils :as utils]
   [conduit.utils.hyper :refer [hyper]]
   [conduit.utils.dep-macro :refer [defact]]
   [conduit.infra.middleware.flash :refer [push-flash]]))

(defhtml auth-component [{:keys [register?]}]
  [:div.auth-page
   [:div.container.page
    [:div.row
     [:div.col-md-6.offset-md-3.col-xs-12
      [:h1.text-xs-center
       (if register?
         "Sign up"
         "Sign in")]
      [:p.text-xs-center
       {:hx-boost "true"}
       (if register?
         [:a {:href "/login"} "Have an account?"]
         [:a {:href "/register"} "Need an account?"])]
      [:ul.error-messages {:id "errors" :hidden ""}]
      [:form
       {:id "authen"
        :hx-post (if register? "/register" "/login")
        :hx-target "body"
        :hx-swap "outerHTML"
        :hx-push-url "true"
        :_ (hyper "on submit set { hidden: true } on #errors")}
       (when register?
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
        "Sign in"]]]]]])

(defn get-login-page [request]
  (if (:identity request)
    (response/redirect "/")
    {:render {:title "Sign in"
              :content (auth-component {:register? false})}}))

(defact ->post-login-page [{:keys [login]}]
  {:pre [(fn? login)]}
  [{:keys [params]}]
  (match (login params)
    [:error error] (utils/list-errors-response {:login error})
    [:ok {:keys [user-id]}] (-> (response/redirect "/")
                                (push-flash :success "Welcome!")
                                (update :session assoc :identity user-id))))

(defn get-register-page [request]
  (if (:identity request)
    (response/redirect "/")
    {:render {:title "Sign up"
              :content (auth-component {:register? true})}}))

(defact ->post-signup [{:keys [register]}]
  {:pre [(fn? register)]}
  [request]
  (let [params (:params request)
        _ (timbre/info "params" params)]
    (match (register params)
      [:error error] (do
                       (timbre/info "registering error: " error)
                       (utils/list-errors-response {:register error}))
      [:ok user] (do
                   (timbre/info "user: " user)
                   (-> (response/redirect "/")
                       (push-flash :success "Welcome!")
                       (update :session assoc :identity (:user-id user)))))))

(defn ->auth-routes [user-service]
  [""
   ["register"
    {:name :register
     :get get-register-page
     :post
     {:handler (->post-signup user-service)
      :parameters {:form
                   [:map
                    {:closed true}
                    [:email :email]
                    [:username [:string {:min 4 :max 32}]]
                    [:password :password]]}}}]
   ["login"
    {:name ::login
     :get get-login-page
     :post
     {:handler (->post-login-page user-service)
      :parameters {:form
                   [:map
                    {:closed true}
                    [:email :email]
                    [:password :password]]}}}]
   ["logout"
    {:name :logout
     :post
     {:handler
      (fn [request]
        (let [session (->
                       (:session request)
                       (dissoc :identity)
                       (dissoc :user)
                       (dissoc :user-id))]
          (->
           (response/redirect "/")
           (assoc :session session))))}}]])
