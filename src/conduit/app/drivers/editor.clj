(ns conduit.app.drivers.editor
  (:require
   [clojure.string :as str]
   [clojure.core.match :refer [match]]
   [java-time.api :as jt]
   [ring.util.response :as response]
   [conduit.utils.hyper :refer [hyper]]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.infra.utils :as utils]
   [conduit.core.services.article :as article-service :refer [favorite unfavorite]]))

(defhtml editor-comp [{:keys [article new?]}]
  [:div.editor-page
   [:div.container.page
    [:div.row
     [:div.col-md-10.offset-md-1.col-xs-1
      [:ul#errors.error-messages {:hidden "true"}]

      [:form
       [:fieldset

        [:fieldset.form-group
         [:input#text.form-control.form-control-lg
          {:type "text"
           :name "title"
           :placeholder "Article Title"}]]
        
        [:fieldset.form-group
         [:input.form-control
          {:type "text"
           :name "description"
           :placeholder "What's this article about?"}]]

        [:fieldset.form-group
         [:textarea.form-control
          {:rows "8",
           :name "body"
           :placeholder "Write your article (in markdown)"}]]

        [:fieldset.form-group
         [:input#tag-input.form-control
          {:_ (hyper "
on keyup
  if (my.value).length >= 3
    log 'leng ' + (my.value).length
    log 'code ' + event.keyCode
    if the event's key is 'Enter' 
      halt the event
      set taginput to my.value
      send newTag(tag: taginput) to #tags
      set my.value to ''

    else 
      if the event's keyCode is 188 and (my.value).length >= 4
        halt the event
        set taginput to my.value.slice(0, -1)
        send newTag(tag: taginput) to #tags
        send newTag(tag: taginput) to #tags-list
        set my.value to ''
                     ")
           :type "text",
           :placeholder "Enter tags"}]
         [:input#tags
          (assoc
            {:_ (hyper "
on newTag(tag)
  log `new tag: $tag`
  call (my.value).split(',')
  make a Set from it called tagsSet
  call tagsSet.add(tag)
  call tagsSet.delete('')
  call Array.from(tagsSet) then set my.value to it.join(',')
  send updateTags to #tags-list
end

on deleteTag(tag)
  log `delete tag: $tag`
  call (@value of me).split(',')
  make a Set from it called tagsSet
  log 'current val ' + (@value of me)
  log 'tags before delete ' + tagsSet
  call tagsSet.delete(tag)
  call tagsSet.delete('')
  call Array.from(tagsSet) then set my @value to it.join(',')
  send updateTags to #tags-list
end
                     ")
             :hidden "true"}
            :value (if new? "" (str/join "," (:tags article))))]
         [:div#tags-list.tags-list
          {:_ (hyper "
on click
  log 'click'
  if target matches .ion-close-round
    set tag to target.parentElement.tag
    remove target.parentElement
  else if target matches .tag-pill
    set tag to target.tag
    remove target
  end
  
  if tag then
    send deleteTag(tag: tag of target) to #tags
  end
end

on updateTags or load
  remove my children
  log `update tags`
  set tags to (#tags).value
  log `tags: $tags`

  repeat for tag in tags.split(`,`).filter(Boolean)
    make a <span.tag-default.tag-pill /> called tagEl
    set { tag: tag } on tagEl
    make <i.ion-close-round /> then put it into tagEl
    append tag to tagEl
    put tagEl at the end of #tags-list
                     ")}]]
        [:button#submit.btn.btn-lg.pull-xs-right.btn-primary
         (assoc
           {:type "button"}
           (if new? :hx-post :hx-put) 
           (if new? "/articles" (str "/articles/" (:slug article))))
         "Publish Article"]]]]]]])

(defn ->get-editor [article-service]
  (fn [request]
    (let [slug (get-in request [:parameters :path :slug])
          new? (not (nil? slug))]
      {:render {:title "New Article"
                :content (editor-comp {:new? new?
                                       :article {}})}})))

(defn ->editor-routes [article-service]
  ["editor"
   ["" {:name :editor/new
        :get {:parameters {:query [:map
                                   {:closed true}
                                   [:limit {:optional true} :int]
                                   [:offset {:optional true} :int]
                                   [:tag {:optional true} :string]
                                   [:author {:optional true} :string]
                                   [:favorited {:optional true} :string]]}
              :handler (->get-editor article-service)}}]
   ["/:slug" {:name :editor/update
              :conflicting true
              :parameters {:path [:map {:closed true}
                                  [:slug :string]]}
              :handler (->get-editor article-service)}]])
