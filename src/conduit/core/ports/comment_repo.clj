(ns conduit.core.ports.comment-repo)

(defprotocol comment-repository
  (create
    [repo params]
    "Create a comment"))
