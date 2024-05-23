(ns conduit.app.drivers.editor
  (:require
   [clojure.string :as str]
   [clojure.core.match :refer [match]]
   [ring.util.response :as response]
   [conduit.utils.hyper :refer [hyper]]
   [conduit.infra.hiccup :refer [defhtml]]
   [conduit.infra.middleware.flash :refer [push-flash]]
   [conduit.core.services.article :as article-service :refer [find-article]]))

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
          (assoc
            {:type "text"
             :name "title"
             :placeholder "Article Title"}
            :value (if new? nil (:title article)))]]
        
        [:fieldset.form-group
         [:input.form-control
          (assoc
            {:type "text"
             :name "description"
             :placeholder "What's this article about?"}
            :value (if new? nil (:description article)))]]

        [:fieldset.form-group
         [:textarea.form-control
          {:rows "8",
           :name "body"
           :placeholder "Write your article (in markdown)"}
          (if new? nil (:body article))]]

        [:fieldset.form-group
         [:input#tag-input.form-control
          {:_ (hyper "
on keyup
  if (my.value).length >= 3
    if the event's key is 'Enter' 
      halt the event
      set taginput to my.value.trim()
      send newTag(tag: taginput) to #tags
      set my.value to ''

    else 
      if the event's keyCode is 188 and (my.value).length >= 4
        halt the event
        set taginput to my.value.slice(0, -1).trim()
        send newTag(tag: taginput) to #tags
        set my.value to ''
                     ")
           :type "text",
           :placeholder "Enter tags"}]
         [:input#tags
          (assoc
            {:_ (hyper "
on newTag(tag)
  call (my.value).split(',')
  make a Set from it called tagsSet
  call tagsSet.add(tag)
  call tagsSet.delete('')
  call Array.from(tagsSet) then set my.value to it.join(',')
  send updateTags to #tags-list
end

on deleteTag(tag)
  call (#tags.value).split(',')
  make a Set from it called tagsSet
  call tagsSet.delete(tag)
  call tagsSet.delete('')
  call Array.from(tagsSet) then set #tags.value to it.join(',')
  send updateTags to #tags-list
end
                     ")
             :value (if new? nil (str/join "," (:tags article)))
             :hidden true
             :name "tags"}
             
            :value (if new? "" (str/join "," (:tags article))))]
         [:div#tags-list.tags-list
          {:_ (hyper "
on click
  if target matches .ion-close-round
    set tag to target.parentElement.tag
    remove target.parentElement
  else if target matches .tag-pill
    set tag to target.tag
    remove target
  end
  
  if tag then
    send deleteTag(tag: tag) to #tags
  end
end

on updateTags or load
  remove <span.tag-pill /> from me
  call (#tags.value).split(',')
  make a Set from it called tagsSet
  call tagsSet.delete('')

  repeat for tag in tagsSet
    make a <span.tag-default.tag-pill /> called tagEl
    set { tag: tag } on tagEl
    make <i.ion-close-round /> then put it into tagEl
    append '  ' to tagEl
    append tag to tagEl
    put tagEl at the end of #tags-list
                     ")}]]
        [:button#submit.btn.btn-lg.pull-xs-right.btn-primary
         (assoc
           {:type "button"
            :hx-swap "outerHTML"
            :hx-target "body"
            :hx-push-url "true"}
           (if new? :hx-post :hx-put) 
           (if new? "/articles" (str "/articles/" (:slug article))))
         "Publish Article"]]]]]]])

(defn ->get-editor [edit? service]
  (fn [request]
    (let [user-id (get request :user-id)
          slug (get-in request [:parameters :path :slug])]
      (if (not edit?)
        {:render {:title "New Article"
                  :content (editor-comp {:new? true
                                         :article {}})}}
        (match (find-article service user-id slug)
          [:error _error] (-> (response/redirect "/" :see-other)
                              (push-flash :warning (str "No article found for '" slug "'")))
          [:ok article] {:render {:title (str "Edit " (:title article))
                                  :content (editor-comp {:new? false
                                                         :article article})}})))))

(defn ->editor-routes [article-service]
  ["editor"
   ["" {:name :editor/new
        :get {:handler (->get-editor false article-service)}}]
   ["/:slug" {:name :editor/edit
              :conflicting true
              :parameters {:path [:map {:closed true}
                                  [:slug :string]]}
              :handler (->get-editor true article-service)}]])
