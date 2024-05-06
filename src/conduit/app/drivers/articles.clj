(ns conduit.app.drivers.articles
  (:require
   [java-time.api :as jt]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.core.services.article :as article-service]
   [conduit.infra.utils :as utils]))
   

(defhtml article-preview [{:keys [title description tags created-at]}]
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
      (jt/format "MMMM d, YYYY" (jt/zoned-date-time created-at (jt/zone-id)))]]]
   [:a.preview-link
    {:href "#"}
    [:h1 title]
    [:p description]]
   [:ul.tag-list
    (for [tag tags]
      [:li.tag-default.tag-pill.tag-outline
       tag])]])

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


(defn ->get-articles [article-service]
  (fn [_request]
    (let [articles (article-service/list article-service 0 {:limit 10 :offset 0})
          ; TODO: add following
          no-following? false
          res (list-articles {:articles articles
                              :no-following? no-following?})]
       (->
         res
         (utils/response)))))

(defn ->articles-routes [article-service]
  ["articles"
   {:name :get-articles
    :get (->get-articles article-service)}])
