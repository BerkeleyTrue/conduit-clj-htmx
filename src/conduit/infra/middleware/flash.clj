(ns conduit.infra.middleware.flash
  (:require
   [malli.core :as m]))

(def FlashM
  [:map
   [:danger {:optional true} [:sequential :string]]
   [:warning {:optional true} [:sequential :string]]
   [:success {:optional true} [:sequential :string]]
   [:info {:optional true} [:sequential :string]]
   [:primary {:optional true} [:sequential :string]]
   [:secondary {:optional true} [:sequential :string]]
   [:light {:optional true} [:sequential :string]]
   [:dark {:optional true} [:sequential :string]]])

(m/=> push-flash [:=>
                  [:cat
                   [:map [:flash
                          {:optional true}
                          [:maybe FlashM]]]
                   :keyword ; level
                   :string] ; msg

                  [:map [:flash FlashM]]])
(defn push-flash
  "Adds a flash message to the session."
  [response lvl msg]
  (let [flash (or (:flash response) {})
        flash (assoc flash lvl (conj (get flash lvl []) msg))]
    (assoc response :flash flash)))

(m/=> merge-flash [:=>
                   [:cat [:maybe FlashM] [:maybe FlashM]]
                   FlashM])
(defn merge-flash
  "Merge two flash maps."
  [flash1 flash2]
  (reduce-kv (fn [acc lvl msgs]
               (assoc acc lvl (into (get acc lvl []) msgs)))
             (or flash1 {})
             (or flash2 {})))

(defn flash-response-middleware
  "Stores flash messages on the response and request in the session.
  If the response contains a :flash/delete key, the flash messages are deleted."
  [handler]
  (fn
    [request]
    (when-let [response (handler request)]
      (let [{session :session} request
            prev-flash (:flash session)
            session (if (contains? response :session)
                      (:session response)
                      session)
            new-flash (:flash response)
            delete-flash (contains? response :flash/delete)
            session (cond
                      delete-flash (dissoc session :flash)
                      new-flash (assoc
                                 (get response :session session)
                                 :flash
                                 (merge-flash prev-flash new-flash))
                      :else session)]
        (if (or delete-flash
                (:flash response)
                (contains? response :session))
          (assoc response :session session)
          response)))))
