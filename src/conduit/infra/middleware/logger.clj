(ns conduit.infra.middleware.logger
  "Middleware to log each request, response, and parameters."
  (:require
    [taoensso.timbre :as timbre]
    [ring.util.response :as response]))

(defn logger
  "Request logger middleware. Logs response time, mehtod, uri, and status code."
  [handler]
  (fn logger-handler [request]
    (let [start-ms (System/currentTimeMillis)
          method (:request-method request)
          uri (:uri request)]
      (try
        (let [response (handler request)
              end-ms (System/currentTimeMillis)
              status (:status response)
              response-time (str (- end-ms start-ms) "ms")]
          (timbre/info (str method " " uri " " status " " response-time))
          response)
        (catch Exception ex
          (let [end-ms (System/currentTimeMillis)
                response-time (str (- end-ms start-ms) "ms")]
            (timbre/error ex (str method " " uri " " " " response-time "ms - Error"))
            (->
              (response/response "Internal Server Error")
              (response/status 500))))))))
