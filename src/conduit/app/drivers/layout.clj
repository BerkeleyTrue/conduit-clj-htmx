(ns conduit.app.drivers.layout
  (:require
   [hiccup.util :as util]
   [conduit.app.drivers.hot-reload :refer [hot-reload-script]]
   [conduit.utils.hyper :refer [hyper]]
   [conduit.infra.hiccup :refer [defhtml htmx-csrf]]
   [conduit.infra.utils :as utils]
   [conduit.infra.middleware.flash :refer [merge-flash]]))

(def place-holder "https://static.productionready.io/images/smiley-cyrus.jpg")

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

(defhtml flash-component [[lvl msgs]]
  (when (seq msgs)
    (for [msg msgs]
      [:div.alert.alert-dismissible.container
       {:role "alert" :hidden "true" :class (str "alert-" (name lvl))
        :_ (hyper
            "
            on start
              log 'showing alert'
              set { hidden: false } on me
              transition me opacity to 1
              if " (not= lvl :danger) "  then
                wait 4s
                transition me opacity to 0
                remove me
                send removed to #alerts
              end
            ")}
       msg
       [:button.close
        {:type "button"
         :aria-label "Close"}
        [:span
         {:_ (hyper
              "
              on click
                transition me opacity to 0
                remove closest <div.alert />
                send removed to #alerts
              ")
          :aria-hidden "true"}
         (util/raw-string "&times;")]]])))

(defhtml flashes-component [flashm]
  (when (seq flashm)
    [:div#alerts.fixed
     {:_ (hyper
          "
            init
              wait 0.5s
              send start to first .alert in me
            on removed
              log 'alert removed'
              wait 0.5s
              if my.children is empty then
                log 'alerts empty'
                set { hidden: true } on me
              else
                wait 0.5s
                log 'next alert'
                send start to first first .alert in me
              end
          ")}
     (map flash-component flashm)]))

(defhtml header [{:keys [links user current-uri]}]
  [:nav.navbar.navbar-light
   {:hx-boost "true"
    :hx-push-url "true"}
   [:div.container
    [:a.navbar-brand {:href "/"} "conduit"]
    [:ul.nav.navbar-nav.pull-xs-right
     (map (fn [{:keys [uri title]}]
            [:li.nav-item
             [:a.nav-link
              {:href uri
               :class (when (= uri current-uri) "active")}
              title]])
          links)
     (when (:user-id user)
       [:li.nav-item
        [:a.nav-link
         {:href (str "/profiles/" (:username user))}
         [:img.user-pic {:src (if (seq (:image user)) (:image user) place-holder)}]
         (:username user)]])]]])

(defhtml footer []
  [:footer
   [:div.container
    [:a.logo-font {:href "/"} "conduit"]
    [:span.attribution
     "An interactive learning project from"
     [:a {:href "https://thinkster.io"} "Thinkster"]
     ". Code &amp; design licensed\n\t\t\t\tunder MIT."]]])

(defhtml layout
  [{:keys [links user uri title flashm]} content]
  (let [links (or links [])
        user (or user {})
        current-uri (or uri "/")]
    (list
     (util/raw-string "<!DOCTYPE html>\n")
     [:html.fullscreen
      {:lang "en"
       :data-theme "catppuccin"}
      [:head
       [:meta {:charset "utf-8"}]
       [:title (str "Conduit | " title)]
       [:link
        {:href "http://code.ionicframework.com/ionicons/2.0.1/css/ionicons.min.css",
         :rel "stylesheet",
         :type "text/css"}]
       [:link
        {:href "https://fonts.googleapis.com/css?family=Titillium+Web:700|Source+Serif+Pro:400,700|Merriweather+Sans:400,700|Source+Sans+Pro:400,300,600,700,300italic,400italic,600italic,700italic",
         :rel "stylesheet",
         :type "text/css"}]
       [:link
        {:rel "icon",
         :type "image/x-icon",
         :href "https://www.realworld.how/img/favicon.ico"}]
       (comment "Import the custom Bootstrap 4 theme from our hosted CDN")
       [:link
        {:rel "stylesheet",
         :href "https://demo.productionready.io/main.css"}]
       [:script
        {:src "https://unpkg.com/htmx.org@1.9.5",
         :integrity
         "sha384-xcuj3WpfgjlKF+FXhSQFQ0ZNr39ln+hwjN3npfM9VBnUskLolQAcN80McRIVOPuO",
         :crossorigin "anonymous"}]
       [:script
        {:src "https://unpkg.com/hyperscript.org@0.9.11",
         :crossorigin "anonymous"}]
       [:style
        ".fixed {
           position: fixed;
           top: 0;
           z-index: 1020;
           width: 100%;
         }"]
       (hot-reload-script)]
      [:body
       {:_ (hyper
             "
          on every htmx:afterRequest
            log 'htmx:afterRequest'
            if event.detail.successful
              if #htmx-alert
                then set {hidden: true} on #htmx-alert
              end
            else if event.detail.failed and event.detail.xhr
              log `server error: ${event.detail.xhr.status} - ${event.detail.xhr.statusText}`
              if #htmx-alert
                then set {hidden: false, innerText: 'Oops, something went wrong with the server...'} on #htmx-alert
              end
            else
              log `htmx:afterRequest unknown error`
              if #htmx-alert
                then set {hidden: false, innerText: 'Unexpected error, check your connection and refresh the page'} on #htmx-alert
              end
            end
             ")}
       [:div#htmx-alert.alert.alert-warning.fixed
        {:role "alert"
         :hidden "true"}]
       (flashes-component flashm)
       (header {:links links
                :user user
                :current-uri current-uri})
       content
       (footer)
       (htmx-csrf)]])))

(defn render-middleware
  "Renders the given content with the layout."
  [handler]
  (fn render-handler [request]
    (let [response (handler request)]
      (if (not (:render response))
        response
        (let [prev-flash (get-in request [:session :flash])
              next-flash (:flash response)
              flashm (merge-flash prev-flash next-flash)
              {:keys [content title]} (:render response)
              page (:page request)
              uri (:uri request)
              user (:user request)
              user-id (:user-id request)
              links (if (:user request) authed-links unauthed-links)]
          (->
           (layout
            {:links links
             :page page
             :user-id user-id
             :user user
             :uri uri
             :title title
             :flashm flashm}
            content)
           (utils/response)
           (assoc :flash/delete true)))))))
