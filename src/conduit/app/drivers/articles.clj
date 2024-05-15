(ns conduit.app.drivers.articles
  (:require
   [clojure.core.match :refer [match]]
   [java-time.api :as jt]
   [taoensso.timbre :as timbre]
   [ring.util.response :as response]
   [conduit.utils.hyper :refer [hyper]]
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

; TODO: pagination logic
(defhtml list-articles [{:keys [articles no-following? num-of-articles]}]
  (list
    (if (empty? articles)
      [:div.article-preview
       (if no-following?
         "Follow some authors to see their articles here."
         "No articles are here... yet.")]
      [:div.article-preview
       (for [article articles]
         (article-preview article))])
    (when (> num-of-articles 10)
      [:ul.pagination
       (for [page (range 1 (+ (/ num-of-articles 10) 1))]
        [:li.page-item
         [:a.page-link {:href "#"} page]])])))

(defn ->get-articles [article-service]
  (fn [{:keys [parameters user-id] :as _request}]
    (let [{:keys [limit offset tag favorited author]} (or (:query parameters) {})
          {:keys [articles num-of-articles]} (article-service/list-articles
                                              article-service
                                              user-id
                                              {:limit limit
                                               :offset offset
                                               :tag tag
                                               :favorited-by favorited
                                               :authorname author})
          res (list-articles {:articles articles
                              :no-following? false
                              :num-of-articles num-of-articles})]
      (utils/response res))))

(defn ->get-feed [article-service]
  (fn [{:keys [parameters user-id] :as _reques}]
    (let [{:keys [limit offset]} (or (:query parameters) {})
          {:keys [articles num-of-articles]} (article-service/list-articles
                                              article-service
                                              user-id
                                              {:feed? true
                                               :limit limit
                                               :offset offset})
          res (list-articles {:articles articles
                              :no-following? (empty? articles)
                              :num-of-articles num-of-articles})]
      (utils/response res))))


(defhtml actions-comp [{:keys [my-article? following? favorited? username slug num-of-favs]}]
  (if (not my-article?)
    (list
     [:button#follow-btn.btn.btn-sm.btn-outline-secondary.follow-btn
      (-> {:_ (hyper "
                on htmx:afterRequest[detail.successful]
                  log 'updating'
                  send update to #article-page
                ")
           :hx-swap "none"}
          (assoc (if following? :hx-delete :hx-post) (str "/profiles/" username "/follow")))
      [:i.ion-plus-round]
      (str " " (if following? "Unfollow " "Follow ") username)]
     "  "
     [:button.btn.btn-sm.btn-outline-primary
      (-> {:_ (hyper "on htmx:afterRequest[detail.successful]
                        log 'updateing'
                        send update to #article-page")
           :hx-swap "none"}
          (assoc (if favorited? :hx-delete :hx-post) (str "/articles/" slug "/favorite")))
      [:i.ion-heart]
      (str " " (if favorited? "Unfavorite" "Favorite") " article ")
      [:span.counter (str num-of-favs)]])
    (list
     [:button.btn.btn-sm.btn-outline-secondary
      {:hx-get (str "/editor/" slug)
       :hx-target "body"
       :hx-push-url "true"}
      [:i.ion-edit]
      "Edit Article"]
     [:button.btn.btn-sm.btn-outline-danger
      {:hx-get (str "/editor/" slug)
       :hx-target "body"
       :hx-push-url "true"}
      [:i.ion-trash-a]
      "Delete Article"])))

(defhtml article-meta-comp [{:keys [key oob? authed? my-article? following? favorited?]}
                            {:keys [slug created-at num-of-favs]}
                            {:keys [image username]}]
  [:div.article-meta
   (-> {:id key}
       (#(if oob? (assoc % :hx-oob "true") %)))
   [:a {:href (str "/profiles/" username)}
    [:img {:src image}]]
   [:div.info
    [:a.author {:href (str "/profiles/" username)} username]
    [:span.date
     (jt/format "MMMM d, YYYY" (jt/zoned-date-time created-at (jt/zone-id)))]]
   (when authed?
     (actions-comp {:my-article? my-article?
                    :following? following?
                    :favorited? favorited?
                    :username username
                    :slug slug
                    :num-of-favs num-of-favs}))])

(defhtml article-oob-comp [article my-article?]
  (list
   (article-meta-comp {:key "article-meta-banner"
                       :oob? true
                       :authed? false
                       :my-article? my-article?}
                      article
                      (:author article))
   (article-meta-comp {:key "article-meta-content"
                       :oob? true
                       :authed? false
                       :my-article? my-article?}
                      article
                      (:author article))))

(defhtml article-comp [authed? {:keys [slug title author body tags] :as article}]
  [:div#article-page.article-page
   {:_ (hyper "on update log 'article meta update'")
    :hx-get (str "/articles/" slug "?oob=true")
    :hx-trigger "update"
    :hx-swap "none"
    :hx-disinherit "*"}
   [:div.banner
    [:div.container
     [:h1 title]
     (article-meta-comp
      {:key "article-meta-banner"
       :oob?  false
       :authed? authed?}
      article
      author)]]
   [:div.container.page
    [:div.row.article-content
     [:div.col-md-12
      [:p body]
      [:ul.tag-list
       (for [tag tags]
         [:li.tag-default.tag-pill.tag-outline tag])]]]]
   [:hr]

   [:div.article-actions
    (article-meta-comp {:key "article-meta-content"
                        :oob? false
                        :authed? authed?}
                       article
                       author)]

   [:div.row
    [:div.col-xs-12.col-md-8.offset-md-2
     (if authed?
       [:form.card.comment-form
        {:_ (hyper "on htmx:afterRequest[detail.successful] call me.reset()")
         :hx-post (str "/articles/" slug "comments")
         :hx-target "#comments"
         :hx-swap "beforeend"}
        [:div.card-block
         [:textarea.form-control
          {:name "body"
           :placeholder "Write a comment..."
           :row "3"}]]
        [:div.card-footer
         [:img.comment-author-img
          {:src (:image author)}]
         [:button.btn.btn-sm.btn-primary "Post a Comment"]]]
       [:p
        [:a
         {:href "/login"
          :ui-sref "app.login"}
         "Sign in"]
        "or"
        [:a
         {:href "/register"
          :ui-sref "app.register"}
         "sign up"]
        "to add comments on this article."])
     [:div#comments
      {;:hx-get (str "/articles/" slug "/comments") ; TODO: add comments
       :hx-trigger "load delay:150ms"}
      "Loading comments..."]]]])

(defn ->get-article [article-service]
  (fn [request]
    (let [slug (get-in request [:parameters :path :slug])
          oob? (get-in request [:parameters :query :oob])]
      (match (article-service/find-article article-service {:slug slug})
        [:ok article] (let [username (:username request)
                            authed? (not (nil? (:user-id request)))
                            my-article? (= (get-in article [:author :username]) username)]
                        {:render {:title (:title article)
                                  :content (if oob?
                                             (article-oob-comp article my-article?)
                                             (article-comp authed? article))}})
        [:error error] (do
                         (timbre/info (str "Error fetching article " error))
                         (response/redirect "/" 303))))))

(defn ->articles-routes [article-service]
  ["articles"
   ["" {:name :articles/list
        :get {:parameters {:query [:map
                                   {:closed true}
                                   [:limit {:optional true} :int]
                                   [:offset {:optional true} :int]
                                   [:tag {:optional true} :string]
                                   [:author {:optional true} :string]
                                   [:favorited {:optional true} :string]]}
              :handler (->get-articles article-service)}}]
   ["/feed"
    {:name :articles/feed
     :conflicting true
     :middleware [:authorize]
     :get {:parameters
           {:query [:map
                    {:closed true}
                    [:limit {:optional true} :int]
                    [:offset {:optional true} :int]]}

           :handler (->get-feed article-service)}}]
   ["/:slug" {:name :articles/get
              :conflicting true
              :get {:parameters {:path [:map {:closed true}
                                        [:slug :string]]
                                 :query [:map {:closed true}
                                         [:oob {:optional true} :boolean]]}
                    :handler (->get-article article-service)}}]])
