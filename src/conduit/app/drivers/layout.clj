(ns conduit.app.drivers.layout
  (:require
   [hiccup2.core :as h]
   [hiccup.page :as page]))

(defn header []
  [:nav.navbar.navbar-light
   {:hx-boost "true"
    :hx-push-url "true"}
   [:div.container
    [:a.navbar-brand {:href "/"} "conduit"]
    [:ul.nav.navbar-nav.pull-xs-right]]])

(defn footer []
  [:footer
   [:div.container
    [:a.logo-font {:href "/"} "conduit"]
    [:span.attribution
     "An interactive learning project from"
     [:a {:href "https://thinkster.io"} "Thinkster"]
     ". Code &amp; design licensed\n\t\t\t\tunder MIT."]]])

(defn layout [content]
  (str
   (h/html
    (page/doctype :html5)
    [:html.fullscreen
     {:lang "en"
      :data-theme "catppuccin"}
     [:head
      [:meta {:charset "utf-8"}]
      [:title "Conduit"]
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
        }"]]
     [:body
      {:_
       "
        on every htmx:afterRequest
		      log `htmx:afterRequest`
		      if event.detail.successful
		        if #htmx-alert
		          then set {hidden: true} on #htmx-alert
		        end
		      else if event.detail.failed and event.detail.xhr
            log `server error: ${event.detail.xhr.status} - ${event.detail.xhr.statusText}`
            if #htmx-alert
              then set {hidden: false, innerText: &#39;Oops, something went wrong with the server...&#39;} on #htmx-alert
            end
		      else
		        log `htmx:afterRequest unknown error`
		        if #htmx-alert
		          then set {hidden: false, innerText: &#39;Unexpected error, check your connection and refresh the page&#39;} on #htmx-alert
		        end
		      end
       "}
      [:div#htmx-alert.alert.alert-warning.fixed
       {:role "alert"
        :hidden "true"}]
      (header)
      content
      (footer)]])))

(comment
  (layout [:h1 "Hello World"])
  ,)
