(ns conduit.app.drivers.home
  (:require
   [conduit.utils.hyper :refer [hyper]]
   [conduit.infra.hiccup :refer [defhtml]]))

(defhtml homeComponent [{:keys [authed?]}]
  [:div
   {:class "home-page"}
   [:div
    {:class "banner"}
    [:div
     {:class "container"}
     [:h1 {:class "logo-font"} "conduit"]
     [:p "A place to share your knowledge."]]]
   [:div
    {:class "container page"}
    [:div
     {:class "row"}
     [:div
      {:class "col-md-9"}
      [:div
       {:class "feed-toggle"}
       [:ul#tabs.nav.nav-pills.outline-active
        (when authed?
          [:li.nav-item ; only if signed in
           {:_ (hyper
                "
                on click
                  remove .active from .nav-link in #tabs
                  add .active to .nav-link in me
                  set {hidden: true} on #tag-tab
                ")
            :hx-get "/articles/feed?limit=10"
            :hx-trigger "click, load delay:150ms"
            :hx-target "#articles"}
           [:a {:class "nav-link active"} "Your Feed"]])
        [:li.nav-item
         {:hx-get "/articles?limit=10"
          :hx-trigger (if authed? nil "click, load delay:150ms") ; only if not signed in
          :hx-target "#articles"
          :_ "
            on click
              remove .active from .nav-link in #tabs
              add .active to .nav-link in me
              set {hidden: true} on #tag-tab
          "}
         [:a.nav-link
          {:class (if authed? "" "active")} ; only if not signed in
          "Global Feed"]]

        [:li#tag-tab.nav-item
         {:hidden true}
         [:a.nav-link.active ; always active if visible
          "#tag"]]]]
      [:div
       {:id "articles"}
       [:div.article-preview
        "Loading articles..."]]
      [:ul.pagination
       {:id "pagination" :hidden true}]]
     [:div.col-md-3
      [:div.sidebar
       {:hx-get "/tags"
        :hx-target "#tags"
        :hx-trigger "load delay:150ms"}
       [:p "Popular Tags"]
       [:ul#tags.tag-list
        {:_ (hyper 
              "
              on click
                if event.target.tagName == 'A'
                  -- log event.target
                  remove @hidden from #tag-tab
                  remove .active from .nav-link in #tabs
                  put '#' + event.target.innerHTML into <a/> in #tag-tab
                  add .active to <a/> in #tag-tab
              ")}
        "Loading tags..."]]]]]])

(defn get-home-page
  "Returns the home page."
  [request]
  (let [content (homeComponent {:authed? (:user request)})]
    (->
     {:render {:content content
               :title "Home"}})))

(defn ->home-routes []
  [""
   {:name :get-home
    :get get-home-page}])
