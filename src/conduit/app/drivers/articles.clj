(ns conduit.app.drivers.articles
  (:require
    [conduit.infra.hiccup :refer [defhtml]]))


(defhtml article-preview [_]
  [:div.article-preview
   [:div.article-meta
    [:a
     {:href "#"}
     [:img
      {:src "http://i.imgur.com/Qr71crq.jpg"}]]
    [:div.info
     [:a.author
      {:href "#"}
      "Eric Simons"]
     [:span.date
      "January 20th"]]]
   [:a.preview-link
    {:href "#"}
    [:h1 "How to build webapps that scale"]
    [:p "This is the description for the post."]]
   [:ul.tag-list
    [:li.tag-default.tag-pill.tag-outline
     "Web Development"]
    [:li.tag-default.tag-pill.tag-outline
     "JavaScript"]]])

; TODO: show pagination
(defhtml list-articles [{:keys [articles no-following?]}]
  (if (empty? articles)
    [:div.article-preview 
     (if no-following?
       "Follow some authors to see their articles here."
       "No articles are here... yet.")]
    [:div
     [:div.article-preview
      (for [article articles]
        (article-preview article))]]))

(defn ->articles-routes []
  ["articles"
   {:name :list-articles
    :get list-articles}])
