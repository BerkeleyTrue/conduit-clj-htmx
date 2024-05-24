(ns conduit.core.services.comment
  (:require
   [clojure.core.match :refer [match]]
   [camel-snake-kebab.core :as csk]
   [integrant.core :as ig]
   [malli.core :as m]
   [conduit.core.models :refer [Comment]]
   [conduit.core.ports.comment-repo :as repo]
   [conduit.core.services.user :refer [UserProfile get-profile] :as user-service])
  (:import
   [java.util UUID]))

(defprotocol CommentService 
  (create-comment [_ user-id {:keys [body article-id]}] "A user comments on an article")
  (list-comments [_ user-id article-id] "list comments for article")
  (delete-comment [_ user-id comment-id]))

(defn service? [service?]
  (satisfies? CommentService service?))

(defmethod ig/init-key :core.services/article [_ {:keys [repo user-service]}]
  (assert (repo/repo? repo) (str "Comment services expects a comment repository but found " repo))
  (assert (user-service/service? user-service) (str "Comment services expects a user service but found " user-service))
  (reify CommentService
    (create-comment [_ user-id params])))
