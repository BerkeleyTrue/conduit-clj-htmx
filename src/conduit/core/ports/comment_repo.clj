(ns conduit.core.ports.comment-repo)

(defprotocol CommentRepository
  (create [repo {:keys [comment-id body author-id article-id created-at]}] "Create a comment")
  (delete [repo comment-id] "Delete a comment")
  (list-by-article [repo article-id] "Get comments for an article"))

(defn repo? [repo]
  (satisfies? CommentRepository repo))
