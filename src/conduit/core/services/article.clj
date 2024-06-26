(ns conduit.core.services.article
  (:require
   [clojure.core.match :refer [match]]
   [camel-snake-kebab.core :as csk]
   [integrant.core :as ig]
   [malli.core :as m]
   [conduit.core.models :refer [Article]]
   [conduit.core.ports.article-repo :as repo]
   [conduit.core.services.user :refer [UserProfile get-profile] :as user-service])
  (:import
   [java.util UUID]))

(def ArticleOutput
  [:map
   {:title "Article Output"
    :description "An public article"}
   [:title :string]
   [:slug :string]
   [:description :string]
   [:body :string]
   [:tags [:set :string]]

   [:favorited? :boolean]
   [:num-of-favs :int]
   [:author UserProfile]

   [:created-at :instant]
   [:updated-at [:maybe :instant]]])

(defprotocol ArticleService
  (create-article [_ user-id {:keys [title description body tags]}] "Create an article")
  (list-articles [_ user-id {:keys [tag authorname favorited-by limit offset]}] "List articles")
  (get-popular-tags [_] "Get popular tags")
  (find-article [_ user-id slug] "find an article by id or slug")
  (update-article [_ user-id slug {:keys [title description body tags]}] "Update an article")
  (favorite [_ user-id slug] "Favorite an article")
  (unfavorite [_ user-id slug] "Unfavorite an article")
  (delete-article [_ user-id slug] "Delete an article"))

(defn service? [service?]
  (satisfies? ArticleService service?))

(m/=> format-article [:=> [:cat Article UserProfile :int :boolean] ArticleOutput])
(defn format-article [article profile num-of-favs favorited?]
  {:slug (:slug article)
   :title (:title article)
   :description (:description article)
   :body (:body article)
   :tags (:tags article)
   :author profile
   :favorited? favorited?
   :num-of-favs num-of-favs

   :created-at (:created-at article)
   :updated-at (:updated-at article)})

(defmethod ig/init-key :core.services/article [_ {:keys [repo user-service]}]
  (assert (repo/repo? repo) (str "Article services expects a article repository but found " repo))
  (assert (user-service/service? user-service) (str "Article services expects a user service but found " user-service))

  (reify ArticleService
    (create-article [_ user-id params]
      (let [article (repo/create repo
                                 (assoc params
                                        :article-id (UUID/randomUUID)
                                        :created-at (java.time.Instant/now)
                                        :author-id user-id
                                        :slug (csk/->kebab-case (:title params))))]
        (if (nil? article)
          [:error "Couldn't create article"]
          (let [user (get-profile user-service {:user-id user-id})]
            [:ok (format-article article user 0 false)]))))

    (list-articles [_ user-id {:keys [feed? limit offset tag authorname favorited-by]}]
      (let [args (if feed?
                   {:followed-by user-id}
                   {:tag tag
                    :authorname authorname
                    :favorited-by favorited-by})
            res (repo/list repo (assoc args :limit (or limit 10) :offset (or offset 0)))]
        (update res :articles (fn [articles]
                                (->> articles
                                     (map (fn [article]
                                            ; TODO: num-of-favorites 
                                            ; TODO: is favorited
                                            (match (get-profile user-service {:user-id user-id
                                                                              :author-id (:author-id article)})
                                              [:ok profile] (format-article article profile (rand-int 10) (rand-nth [true false]))
                                              ; NOTE: handle no user?
                                              :else article))))))))
    (get-popular-tags [_]
      [:ok (repo/get-popular-tags repo)])

    (find-article [_ user-id slug]
      (if (not slug)
        [:error "Find article expects an id or an article slug but found neither"]
        (let [article (repo/get-by-slug repo slug)]
          (if (nil? article)
            [:error (str "No article found for " slug)]
            (let [profile (match (get-profile user-service {:user-id user-id
                                                            :author-id (:author-id article)})
                            [:ok profile] profile
                            _ {})
                  favs (or (repo/get-num-of-favorites repo (:article-id article)) [])
                  favorited? (contains? favs user-id)]
              [:ok (format-article article profile (count favs) favorited?)])))))

    (favorite [_ user-id slug]
      (if (not slug)
        [:error "Find article expects an id or an article slug but found neither"]
        (let [article (repo/get-by-slug repo slug)]
          (if (nil? article)
            [:error (str "No article found for " slug)]
            (let [author-id (:author-id article)
                  article-id (:article-id article)
                  favs (repo/favorite repo article-id user-id)
                  profile (match (get-profile user-service {:user-id user-id
                                                            :author-id author-id})
                            [:ok profile] profile
                            _ {})
                  favorited? (contains? favs user-id)]
              [:ok (format-article article profile (count favs) favorited?)])))))

    (unfavorite [_ user-id slug]
      (if (not slug)
        [:error "Find article expects an id or an article slug but found neither"]
        (let [article (repo/get-by-slug repo slug)]
          (if (nil? article)
            [:error (str "No article found for " slug)]
            (let [author-id (:author-id article)
                  article-id (:article-id article)
                  favs (repo/unfavorite repo article-id user-id)
                  profile (match (get-profile user-service {:user-id user-id
                                                            :author-id author-id})
                            [:ok profile] profile
                            _ {})
                  favorited? (contains? favs user-id)]
              [:ok (format-article article profile (count favs) favorited?)])))))

    (update-article [_ user-id slug {:keys [title] :as params}]
      (let [{:keys [article-id author-id] :as old-article} (repo/get-by-slug repo slug)
            new-slug (if title
                       (csk/->kebab-case title)
                       (:slug old-article))
            article (repo/update repo article-id (assoc params :slug new-slug))
            favs (repo/unfavorite repo article-id user-id)
            profile (match (get-profile user-service {:user-id user-id
                                                      :author-id author-id})
                      [:ok profile] profile
                      _ {})
            favorited? (contains? favs user-id)]
        (if article
          [:ok (format-article article profile (count favs) favorited?)]
          [:error "Could not update article"])))

    (delete-article [_ user-id slug]
      (if (nil? slug)
        [:error "delete requires a slug but found none"]
        (let [{:keys [article-id author-id]} (repo/get-by-slug repo slug)]
          (if (not (= user-id author-id))
            [:error "user must be the author of article in order to delete"]
            (if-let [_success? (repo/delete repo article-id)]
              [:ok "Article deleted"]
              [:error "Could not delete article"])))))))
