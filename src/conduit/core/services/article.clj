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
  (get-by-slug [_ slug] "Get an article by slug")
  (get-id-from-slug [_ slug] "Get an article id by slug")
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

    (list-articles [_ user-id {:keys [feed? limit offset tag _favorited _authorname]}]
      ; TODO: add fetch author-id for authorname
      ; TODO: add fetch userid for favorited
      (let [args (if feed?
                   {:followed-by user-id}
                   {:tag tag})]
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
      [:ok (repo/get-popular-tags repo)])))
