(ns conduit.infra.flash
  (:require
   [malli.core :as m]))

(def Flash
  [:map
   [:lvl :keyword]
   [:msg :string]])

;; TODO: fix malli schema
(m/=> flash-user [:=>
                  [:cat
                   [:map [:flash {:optional true} [:sequential Flash]]]
                   Flash]
                  [:map [:flash [:sequential Flash]]]])
(defn flash-user [request flash]
  (update request :flash conj flash))
