(ns conduit.app.drivers.home
  (:require
   [conduit.infra.utils :as utils]
   [conduit.app.drivers.layout :refer [layout]]
   [conduit.infra.hiccup :refer [defhtml hyper]]))

(defhtml homeComponent []
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
       [:ul.nav.nav-pills.outline-active
        {:id "tabs"}
        [:li.nav-item ; only if signed in
         (hyper
           "on click
              remove .active from .nav-link in #tabs
              add .active to .nav-link in me
              set {hidden: true} on #tag-tab
           "
           {:hx-get "/articles/feed?limit=10"
            ;:hx-trigger "click load delay:150ms"
            :hx-target "#articles"})
         [:a {:class "nav-link active"} "Your Feed"]]
        [:li.nav-item
         (hyper
           "on click
             remove .active from .nav-link in #tabs
             add .active to .nav-link in me
             set {hidden: true} on #tag-tab
           "
           {:hx-get "/articles?limit=10"
            ;:hx-trigger "click load delay:150ms" ; only if not signed in
            :hx-target "#articles"})
         [:a.nav-link
          {:class "active"} ; only if not signed in
          "Global Feed"]]

        [:li.nav-item
         {:id "tag-tab"
          :hidden true}
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
        :hx-target "#tags"}
        ;:hx-trigger "load delay:150ms"}
       [:p "Popular Tags"]
       [:div.tag-list
        (hyper
          "on click
             if event.target.tagName == 'A'
               -- log event.target
               remove @hidden from #tag-tab
               remove .active from .nav-link in #tabs
               put '#' + event.target.innerHTML into <a/> in #tag-tab
               add .active to <a/> in #tag-tab
          "
          {:id "tags"})
        "Loading tags..."]]]]]])

(defn get-home-page
  "Returns the home page."
  [_]
  (utils/response (layout {:content (homeComponent)})))
