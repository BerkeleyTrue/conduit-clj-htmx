(ns conduit.core.services.article
  (:require
   [integrant.core :as ig]
   [conduit.utils.dep-macro :refer [defact]])
  (:import
   [java.util UUID Date]))

(defact ->create 
  [{:keys [create-article]} {:keys [get-profile]}]
  {:pre [(fn? create-article) (fn? get-profile)]}
  [_params])

(defact ->list 
  [{:keys [list]}]
  {:pre [(fn? list)]}
  [_params]
  (list {}))

(defmethod ig/init-key :core.services/article [_ {:keys [repo user-service]}]
  {:create (->create repo user-service)})
