(ns conduit.core.services.article
  (:require
   [clojure.core.match :refer [match]]
   [java-time.api :as jt]
   [integrant.core :as ig]
   [malli.core :as m]
   [conduit.core.models :refer [Article]]
   [conduit.core.ports.article-repo :as repo]
   [conduit.core.services.user :refer [UserProfile get-profile] :as user-service]))

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
  (create [_ user-id params] "Create an article")
  (list-articles [_ user-id params] "List articles")
  (get-popular-tags [_] "Get popular tags")
  (find-article [_ {:keys [article-id slug]}] "find an article by id or slug")
  (update-article [_ slug username params] "Update an article")
  (favorite [_ slug user-id] "Favorite an article")
  (unfavorite [_ slug user-id] "Unfavorite an article")
  (delete [_ slug] "Delete an article"))

(defn service? [service?]
  (satisfies? ArticleService service?))

(m/=> format-article [:=> [:cat Article UserProfile :int :boolean] ArticleOutput])
(defn format-article [article profile num-of-favorites favorited-by-user]
  {:slug (:slug article)
   :title (:title article)
   :description (:description article)
   :body (:body article)
   :tags (:tags article)
   :author profile
   :favorited? favorited-by-user
   :num-of-favs num-of-favorites

   :created-at (:created-at article)
   :updated-at (:updated-at article)})

(defmethod ig/init-key :core.services/article [_ {:keys [repo user-service]}]
  (assert (repo/repo? repo) (str "Article services expects a article repository but found " repo))
  (assert (user-service/service? user-service) (str "Article services expects a user service but found " user-service))
  (reify ArticleService
    (create [_ user-id params]
      (let [article (repo/create repo
                                 (assoc params
                                        :created-at (str (jt/instant))
                                        :author-id user-id))]
        (if (nil? article)
          [:error "Couldn't create article"]
          (let [user (get-profile user-service {:user-id user-id})]
            [:ok (format-article article user 0 false)]))))

    (list-articles [_ user-id {:keys [feed? limit offset tag authorname favorited-by]}]
      (let [args (if feed?
                   {:followed-by user-id}
                   {:tag tag
                    :authorname authorname
                    :favorited-by favorited-by})]
        (->> (assoc args
                    :limit (or limit 10)
                    :offset (or offset 0))
             (repo/list repo)
             (map (fn [article]
                    ; TODO: num-of-favorites 
                    ; TODO: is favorited
                    (match (get-profile user-service {:author-id (:author-id article)})
                      [:ok profile] (format-article article profile (rand-int 10) (rand-nth [true false]))
                      ; TODO: handle no user?
                      article))))))
    (get-popular-tags [_] 
      [:ok (repo/get-popular-tags repo)])

    (find-article [_ {:keys [article-id slug]}]
      (if (not (or article-id slug))
        [:error "Find article expects an id or an article slug but found neither"]
        (let [article (cond 
                        slug (repo/get-by-slug repo slug)
                        article-id (repo/get-by-id repo article-id))]
          (if (nil? article)
            [:error (str "No article found for " (or slug article-id))]

            (match (get-profile  user-service {:author-id (:author-id article)})
              [:ok profile] [:ok (format-article article profile (rand-int 10) false)]
              article)))))))
