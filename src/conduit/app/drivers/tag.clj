(ns conduit.app.drivers.tag
  (:require
   [clojure.core.match :refer [match]]
   [conduit.infra.utils :as utils]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.core.services.article :refer [service? get-popular-tags]]))

(defhtml pop-tags [tags]
  (if (empty? tags)
    [:div#tags.tag-list
     "No tags are here... yet,"]
    (for [tag tags]
      [:a.tag-default.tag-pill
       {:hx-get (str "/articles?tag=" tag)
        :hx-target "#articles"
        :hx-swap "innerHTML"}
       tag])))

(defn ->get-popular-tags [articles-service]
  (fn [_request]
    (match (get-popular-tags articles-service)
      [:error error] error
      [:ok tags]
      (-> (pop-tags tags)
          (utils/response)))))
  

(defn ->tag-routes [articles-service]
  (assert (service? articles-service) 
          (str "tag routes expects an article service but found " articles-service))
  ["tags"
   {:name ::tags
    :get (->get-popular-tags articles-service)}])
