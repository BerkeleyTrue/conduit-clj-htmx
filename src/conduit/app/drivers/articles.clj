(ns conduit.app.drivers.articles
  (:require
   [clojure.core.match :refer [match]]
   [clojure.string :as str]
   [java-time.api :as jt]
   [taoensso.timbre :as timbre]
   [ring.util.response :as response]
   [conduit.utils.hyper :refer [hyper]]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.infra.utils :as utils]
   [conduit.infra.middleware.flash :refer [push-flash]]
   [conduit.core.services.article :refer [create-article find-article list-articles update-article favorite unfavorite delete-article]]
   [conduit.core.services.comment :refer [create-comment list-comments delete-comment]]))

(defhtml article-preview [{:keys [title slug description tags created-at author]}]
  (let [{:keys [image username]} author]
    [:div.article-preview
     [:div.article-meta
      [:a
       {:href (str "/profiles/" username)
        :hx-boost "true"}
       [:img
        {:src image}]]
      [:div.info
       [:a.author
        {:href (str "/profiles/" username)
         :hx-boost "true"}
        username]
       [:span.date
        (jt/format "MMMM d, YYYY" (jt/zoned-date-time created-at (jt/zone-id)))]]]
     [:a.preview-link
      {:href (str "/articles/" slug)
       :hx-boost "true"}
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

(defhtml articles-comp [{:keys [no-following? feed? articles num-of-articles cur-page]}]
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

          {:keys [articles num-of-articles page]} (list-articles
                                                   article-service
                                                   user-id
                                                   {:limit limit
                                                    :offset offset
                                                    :tag tag
                                                    :favorited-by favorited
                                                    :authorname author})
          res (articles-comp {:articles articles
                              :no-following? false
                              :num-of-articles num-of-articles
                              :cur-page page})]
      (utils/response res))))

(defn ->get-feed [article-service]
  (fn [{:keys [parameters user-id]}]
    (let [{:keys [limit offset]} (get parameters :query {})
          {:keys [articles num-of-articles page]} (list-articles
                                                   article-service
                                                   user-id
                                                   {:feed? true
                                                    :limit limit
                                                    :offset offset})
          res (articles-comp {:feed? true
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
        {:hx-delete (str "/articles/" slug)
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
                       {:keys [slug title author body tags] :as article}
                       {:keys [image]}]
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
          {:src image}]
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
      {:hx-get (str "/articles/" slug "/comments")
       :hx-trigger "load delay:150ms"}
      "Loading comments..."]]]])

(defn ->get-article [article-service]
  (fn [request]
    (let [slug (get-in request [:parameters :path :slug])
          oob? (get-in request [:parameters :query :oob])
          user-id (:user-id request)]
      (match (find-article article-service user-id slug)
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
                                              article
                                              (:user request))}})) 
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

(defn ->unfav-article [user-service]
  (fn [request]
    (let [slug (get-in request [:parameters :path :slug])
          user-id (get request :user-id)]
      (match (unfavorite user-service user-id slug)
        [:error error] (response/bad-request (str error))
        [:ok _] (response/status 200)))))

(defn ->create-article [service]
  (fn [request]
    (let [user-id (:user-id request)
          params (get-in request [:parameters :form])
          params (if (:tags params)
                   (assoc params :tags (str/split (:tags params) #","))
                   params)]

      (if (nil? params)
        (utils/list-errors-response {:article "No parameters found"})
        (match (create-article service user-id params)
          [:error error] (utils/list-errors-response {:article error})
          [:ok article] (-> (response/redirect (str "/editor/" (:slug article)) :see-other)
                            (push-flash :success "Article created successfully")))))))

(defn ->update-article [service]
  (fn [request]
    (let [user-id (:user-id request)
          slug (get-in request [:parameters :path :slug])
          params (get-in request [:parameters :form])
          params (if (:tags params)
                   (assoc params :tags (str/split (:tags params) #","))
                   params)]

      (if (nil? params)
        (utils/list-errors-response {:article "No parameters found"})
        (match (update-article service user-id slug params)
          [:error error] (utils/list-errors-response {:article error})
          [:ok article] (-> (response/redirect (str "/editor/" (:slug article)) :see-other)
                            (push-flash :success "Article updated successfully")))))))

(defn ->delete-article [service]
  (fn [request]
    (let [author-id (:user-id request)
          authorname (:username request)
          slug (get-in request [:parameters :path :slug])]

      (match (delete-article service author-id slug)
        [:error error] (-> (utils/list-errors-response {:article error})
                           (push-flash :warning "Could not delete article"))
        [:ok _] (-> (response/redirect (str "/profiles/" authorname) :see-other)
                    (push-flash :info "Article deleted"))))))

(defhtml comment-comp [slug {:keys [body author? comment-id created-at author]}]
  [:div.card
   [:div.card-block
    [:p.card-text
     body]]
   [:div.card-footer
    [:a.comment-author {:href (str "/profiles/" (:username author))}
     [:img.comment-author-img {:src (:image author)}]]
    " "
    [:a.comment-author {:href (str "/profiles/" (:username author))}
     (:username author)]
    [:span.date-posted
     (when created-at
       (jt/format "MMMM d, YYYY" (jt/zoned-date-time created-at (jt/zone-id))))]
    (when author?
      [:span.mod-options
       {:_ (hyper "on htmx:afterRequest[detail.successful] remove the closest .card")
        :hx-delete (str "/articles/" slug "/comments/" comment-id)}
       [:i.ion-trash-a]])]])

(defhtml comments-comp [slug comments]
  [:div
   (for [comment comments]
     (comment-comp slug comment))])

(defn ->get-comments [service]
  (fn [request]
    (let [user-id (get request :user-id)
          slug (get-in request [:parameters :path :slug])]
      (match (list-comments service user-id slug)
        [:error error] (utils/list-errors-response {:comments error})
        [:ok comments] (do 
                         (tap> {:get-comments slug
                                :comments comments})
                         (-> (comments-comp slug comments)
                             (utils/response)))))))

(defn ->create-comment [service]
  (fn [request]
    (let [user-id (get request :user-id)
          slug (get-in request [:parameters :path :slug])
          body (get-in request [:parameters :form :body])]

      (tap> {:->comment slug
             :body body})
      (match (create-comment service user-id slug body)
        [:error error] (utils/list-errors-response {:comments error})
        [:ok comment] (-> (comment-comp slug comment)
                          (utils/response))))))

(defn ->delete-comment [service]
  (fn [request]
    (let [user-id (get request :user-id)
          comment-id (get-in request [:parameters :path :id])]

      (match (delete-comment service user-id comment-id)
        [:error error] (utils/list-errors-response {:comments error})
        [:ok _] (response/status 204)))))

(defn ->articles-routes [article-service comment-service]
  ["articles"
   ["" {:get {:name :articles/list
              :parameters {:query [:map
                                   {:closed true}
                                   [:limit {:optional true} :int]
                                   [:offset {:optional true} :int]
                                   [:tag {:optional true} :string]
                                   [:author {:optional true} :string]
                                   [:favorited {:optional true} :string]]}
              :handler (->get-articles article-service)}

        :post {:name :article/create
               :middleware [:authorize]
               :handler (->create-article article-service)
               :parameters {:form [:map {:closed true}
                                   [:title [:string {:min 4 :max 254}]]
                                   [:description [:string {:min 10 :max 254}]]
                                   [:body [:string {:min 10 :max 254}]]
                                   [:tags :string]]}}}]
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
    ["" {:get {:name :article/read
               :parameters {:query [:map {:closed true}
                                    [:oob {:optional true} :boolean]]}
               :handler (->get-article article-service)}

         :put {:name :article/update
               :middleware [:authorize]
               :handler (->update-article article-service)
               :parameters {:form [:map {:closed true}
                                   [:title [:string {:min 4 :max 254}]]
                                   [:description [:string {:min 4 :max 254}]]
                                   [:body [:string {:min 4 :max 254}]]
                                   [:tags :string]]}}
         :delete {:name :article/delete
                  :middleware [:authorize]
                  :handler (->delete-article article-service)}}]

    ["/favorite" {:name :article/fav
                  :middleware [:authorize]
                  :post {:handler (->fav-article article-service)}
                  :delete {:handler (->unfav-article article-service)}}]
    ["/comments"
     {:middleware [:authorize]}
     ["" {:get {:name :articles/comments
                :handler (->get-comments comment-service)}
          :post {:name :comments/get
                 :parameters {:form [:map [:body [:string {:min 5 :max 254}]]]}
                 :handler (->create-comment comment-service)}}]
     ["/:id" {:parameters {:path [:map [:id :uuid]]}
              :delete {:name :comments/delete
                       :handler (->delete-comment comment-service)}}]]]])
