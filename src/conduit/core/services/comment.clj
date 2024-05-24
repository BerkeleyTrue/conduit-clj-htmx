(ns conduit.core.services.comment
  (:require
   [clojure.core.match :refer [match]]
   [camel-snake-kebab.core :as csk]
   [integrant.core :as ig]
   [malli.core :as m]
   [conduit.core.models :refer [Comment]]
   [conduit.core.ports.comment-repo :as repo]
   [conduit.core.services.article :refer [find-article]]
   [conduit.core.services.user :refer [UserProfile get-profile] :as user-service])
  (:import
   [java.util UUID]))

(defprotocol CommentService
  (create-comment [_ user-id slug body] "A user comments on an article")
  (list-comments [_ user-id slug] "list comments for article")
  (delete-comment [_ user-id comment-id] "A user deletes a comment"))

(defn service? [service?]
  (satisfies? CommentService service?))

(defmethod ig/init-key :core.services/comments [_ {:keys [repo user-service article-service]}]
  (assert (repo/repo? repo) (str "Comment services expects a comment repository but found " repo))
  (assert (user-service/service? user-service) (str "Comment services expects a user service but found " user-service))

  (reify CommentService
    (create-comment [_ user-id slug body]
      (match (find-article article-service user-id slug)
        [:error error] [:error error]

        [:ok article]
        (let [comment (repo/create repo {:author-id user-id
                                         :article-id (:article-id article)
                                         :comment-id (UUID/randomUUID)
                                         :body body
                                         :created-at (java.time.Instant/now)})]
          (if comment
            [:ok comment]
            [:error "Could not create comment"]))))

    (list-comments [_ user-id slug]
      (match (find-article article-service user-id slug)
        [:error error] [:error error]

        [:ok article]
        [:ok (->> (repo/list-by-article repo (:article-id article))
                  (map (fn [{:keys [author-id] :as comment}]
                         (let [profile (match (get-profile user-service {:user-id user-id
                                                                         :author-id author-id})
                                         [:error _error] {}
                                         [:ok profile] profile)]
                           (assoc comment :author profile)))))]))

    (delete-comment [_ user-id comment-id]
      (let [{:keys [author-id]} (or (repo/find-by-id repo comment-id) {})]
        (if (= author-id user-id)
          (let [deleted? (repo/delete repo comment-id)]
            (if deleted?
              [:ok true]
              [:error "Could not delete comment"]))
          [:error "Comment not owned by user"])))))
