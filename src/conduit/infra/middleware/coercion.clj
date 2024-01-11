(ns conduit.infra.middleware.coercion
  (:require
   [reitit.coercion :as coercion]
   [ring.util.response :as response]
   [conduit.infra.utils :refer [list-errors]]))

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
        (response/header "HX-Reswap" "none")
        (response/content-type "text/html"))
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
