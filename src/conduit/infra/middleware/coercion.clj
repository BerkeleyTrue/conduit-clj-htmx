(ns conduit.infra.middleware.coercion
  (:require
   [reitit.coercion :as coercion]
   [ring.util.response :as response]
   [conduit.infra.hiccup :refer [defhtml]]))

(defhtml list-errors [errors-map]
  [:ul#errors.error-messages
   {:hx-swap-oob "true"}
   (for [[k errs] errors-map]
     (for [err errs]
      [:li
       (str (name k) ": " err)]))])

(comment
  (list-errors
    {:email ["is required" "is not a valid email"]
     :password ["is required" "must be at least 8 characters"]}))

(defn handle-coercion-exception [e]
  (let [data (ex-data e)]
    (if-let [status (case (:type data)
                      ::coercion/request-coercion 200 ; htmx will handle displaying errors
                      ::coercion/response-coercion 500
                      nil)]
      (->
       {:status status
        :body (->
                (coercion/encode-error data)
                (:humanized)
                (list-errors)
                (str))}
       (response/header "HX-Push-URL" false)
       (response/header "HX-Reswap" "none"))
      (throw e))))

(def coerce-exceptions-htmx-middleware
  "Middleware for handling coercion exceptions.
  Expects a :coercion of type `reitit.coercion/Coercion`
  and :parameters or :responses from route data, otherwise does not mount.
  Will add htmx headers."
  {:name ::coerce-exceptions
   :compile (fn [{:keys [coercion parameters responses]} _]
              (when (and coercion (or parameters responses))
                (fn [handler]
                  (fn
                    ([request]
                     (try
                       (handler request)
                       (catch Exception e
                         (handle-coercion-exception e))))))))})
