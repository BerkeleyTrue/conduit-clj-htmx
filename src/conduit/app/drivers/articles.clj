(ns conduit.app.drivers.articles
  (:require
   [java-time.api :as jt]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.infra.utils :as utils]
   [conduit.core.services.article :as article-service]))

(defhtml article-preview [{:keys [title slug description tags created-at author]}]
  (let [{:keys [image username]} author]
    [:div.article-preview
     [:div.article-meta
      [:a
       {:href (str "/profiles/" username)}
       [:img
        {:src image}]]
      [:div.info
       [:a.author
        {:href (str "/profiles/" username)}
        username]
       [:span.date
        (jt/format "MMMM d, YYYY" (jt/zoned-date-time created-at (jt/zone-id)))]]]
     [:a.preview-link
      {:href (str "/articles/" slug)}
      [:h1 title]
      [:p description]]
     [:ul.tag-list
      (for [tag tags]
        [:a.tag-default.tag-pill.tag-outline
         ; TODO: load articles by tag
         {:href "#"}
         tag])]]))

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
  (fn [{:keys [parameters user-id] :as _request}]
    (let [{:keys [limit offset tag]} (or (:query parameters) {})
          articles (article-service/list-articles 
                     article-service 
                     user-id
                     {:limit limit 
                      :offset offset 
                      :tag tag})
          no-following? (not (or (not user-id) (seq articles)))
          res (list-articles {:articles articles
                              :no-following? no-following?})]
      (-> res
          (utils/response)))))

(defn ->articles-routes [article-service]
  ["articles"
   {:name :get-articles
    :get {:parameters {:query [:map [:limit {:optional true} :int] 
                                    [:offset {:optional true} :int]
                                    [:tag {:optional true} :string]]}
          :handler (->get-articles article-service)}}])
