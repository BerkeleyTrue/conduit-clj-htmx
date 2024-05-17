(ns conduit.app.drivers.articles
  (:require
   [clojure.core.match :refer [match]]
   [java-time.api :as jt]
   [taoensso.timbre :as timbre]
   [ring.util.response :as response]
   [conduit.utils.hyper :refer [hyper]]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.infra.utils :as utils]
   [conduit.core.services.article :as article-service :refer [favorite]]))

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
      {:_ (hyper "
              on click
                if event.target.tagName == 'A'
                  -- log event.target
                  remove @hidden from #tag-tab
                  remove .active from .nav-link in #tabs
                  put '#' + event.target.innerHTML into <a/> in #tag-tab
                  add .active to <a/> in #tag-tab
                 ")}
      (for [tag tags]
        [:a.tag-default.tag-pill.tag-outline
         {:href "#"
          :hx-get (str "/articles?tag=" tag)
          :hx-target "#articles"
          :hx-swap "innerHTML"}
         tag])]]))

(defhtml list-articles [{:keys [no-following? feed? articles num-of-articles cur-page]}]
  (list
   (if (empty? articles)
     [:div.article-list
      (if no-following?
        "Follow some authors to see their articles here."
        "No articles are here... yet.")]
     [:div.article-list
      (for [article articles]
        (article-preview article))])
   (when (> num-of-articles 10)
     [:ul.pagination
      (for [page (range 1 (+ (/ num-of-articles 10) 1))]
        [:li.page-item {:class (if (= page cur-page) "active" "")}
         [:a.page-link
          (when (not (= page cur-page))
            {:_ (hyper "
                       on htmx:afterRequest[detail.successful]
                         log 'articles updated'
                         go to the top left of #articles smoothly
                       ")
             :href "#"
             :hx-get (str "/articles" (if feed? "/feed" "") "?limit=10&offset=" (* (- page 1) 10))
             :hx-target "#articles"})
          page]])])))

(defn ->get-articles [article-service]
  (fn [{:keys [parameters user-id] :as _request}]
    (let [{:keys [limit offset tag favorited author]} (or (:query parameters) {})

          {:keys [articles num-of-articles page]} (article-service/list-articles
                                                   article-service
                                                   user-id
                                                   {:limit limit
                                                    :offset offset
                                                    :tag tag
                                                    :favorited-by favorited
                                                    :authorname author})
          res (list-articles {:articles articles
                              :no-following? false
                              :num-of-articles num-of-articles
                              :cur-page page})]
      (utils/response res))))

(defn ->get-feed [article-service]
  (fn [{:keys [parameters user-id]}]
    (let [{:keys [limit offset]} (get parameters :query {})
          {:keys [articles num-of-articles page]} (article-service/list-articles
                                                   article-service
                                                   user-id
                                                   {:feed? true
                                                    :limit limit
                                                    :offset offset})
          res (list-articles {:feed? true
                              :no-following? (empty? articles)
                              :articles articles
                              :num-of-articles num-of-articles
                              :cur-page page})]
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
    (when my-article?
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
        "Delete Article"]))))

(defhtml article-meta-comp [{:keys [key oob? my-article? favorited?]}
                            {:keys [slug created-at num-of-favs]}
                            {:keys [image username following?]}]
  [:div.article-meta
   (-> {:id key}
       (#(if oob? (assoc % :hx-swap-oob "true") %)))
   [:a {:href (str "/profiles/" username)}
    [:img {:src image}]]
   [:div.info
    [:a.author {:href (str "/profiles/" username)} username]
    [:span.date
     (jt/format "MMMM d, YYYY" (jt/zoned-date-time created-at (jt/zone-id)))]]
   (actions-comp {:my-article? my-article?
                  :following? following?
                  :favorited? favorited?
                  :username username
                  :slug slug
                  :num-of-favs num-of-favs})])

(defhtml article-oob-comp [article {:keys [my-article? following? favorited?]}]
  (list
   (article-meta-comp {:key "article-meta-banner"
                       :oob? true
                       :my-article? my-article?
                       :following? following?
                       :favorited? favorited?}
                      article
                      (:author article))
   (article-meta-comp {:key "article-meta-content"
                       :oob? true
                       :my-article? my-article?
                       :following? following?
                       :favorited? favorited?}
                      article
                      (:author article))))

(defhtml article-comp [{:keys [authed? my-article? favorited?]}
                       {:keys [slug title author body tags] :as article}]
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
       :favorited? favorited?
       :my-article? my-article?}
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
                        :my-article? my-article?
                        :favorited? favorited?}
                       article
                       author)]

   [:div.row
    [:div.col-xs-12.col-md-8.offset-md-2
     (if authed?
       [:form.card.comment-form
        {:_ (hyper "on htmx:afterRequest[detail.successful] call me.reset()")
         :hx-post (str "/articles/" slug "/comments")
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
         "Sign in "]
        "or"
        [:a
         {:href "/register"
          :ui-sref "app.register"}
         " sign up "]
        "to add comments on this article."])
     [:div#comments
      {;:hx-get (str "/articles/" slug "/comments") ; TODO: add comments
       :hx-trigger "load delay:150ms"}
      "Loading comments..."]]]])

(defn ->get-article [article-service]
  (fn [request]
    (let [slug (get-in request [:parameters :path :slug])
          oob? (get-in request [:parameters :query :oob])
          user-id (:user-id request)]
      (match (article-service/find-article article-service user-id slug)
        [:ok article] (let [username (:username request)
                            authed? (not (nil? (:user-id request)))
                            my-article? (= (get-in article [:author :username]) username)
                            favorited? (:favorited? article)]
                        (if oob?
                          (-> (article-oob-comp article {:my-article? my-article?})
                              (utils/response))
                          {:render {:title (:title article)
                                    :content (article-comp
                                              {:authed? authed?
                                               :my-article? my-article?
                                               :favorited? favorited?}
                                              article)}}))
        [:error error] (do
                         (timbre/info (str "Error fetching article " error))
                         (response/redirect "/" :see-other))))))

(defn ->fav-article [user-service]
  (fn [request]
    (let [slug (get-in request [:parameters :path :slug])
          user-id (get request :user-id)]
      (match (favorite user-service user-id slug)
        [:error error] (response/bad-request (str error))
        [:ok _] (response/status 200)))))

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
   ["/feed" {:name :articles/feed
             :conflicting true
             :middleware [:authorize]
             :get {:parameters {:query [:map {:closed true}
                                        [:limit {:optional true} :int]
                                        [:offset {:optional true} :int]]}

                   :handler (->get-feed article-service)}}]
   ["/:slug" {:conflicting true
              :parameters {:path [:map {:closed true}
                                  [:slug :string]]}}
    ["" {:name :article/get
         :get {:parameters {:query [:map {:closed true}
                                    [:oob {:optional true} :boolean]]}
               :handler (->get-article article-service)}}]
    ["/favorite" {:name :article/fav
                  :post {:handler (->fav-article article-service)}}]]])
    ;               ; :delete {:handler (->unfav-article article-service)}}]]])
