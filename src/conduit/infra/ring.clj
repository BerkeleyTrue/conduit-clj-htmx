(ns conduit.infra.ring
  (:require
   [clojure.core.async :refer [go <!]]
   [clojure.java.io :as io]
   [integrant.core :as ig]
   [ring.util.response :as response]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.core.protocols :as protocols]
   [conduit.env :as env]))

(defmethod ig/init-key :infra.routes/health
  [_ _]
  ["/ping" {:name ::health
            :get (fn [_]
                   (->
                     "pong"
                     (response/response)
                     (response/content-type "text/html")))}])

(derive :infra.routes/health :infra/routes)

(comment
  (isa? :infra.routes/health :infra/routes))

(defmethod ig/init-key :infra.router/routes
  [_ {:keys [infra-routes routes]}]
  (into [] (concat infra-routes routes)))

(defn ->middleware []
  (let [env-middleware (:middleware env/defaults)]
    [#(wrap-defaults % site-defaults)
     env-middleware]))

; extend core.async ManyToManyChannel to rings StreamableResponseBody
; this allows us to send sse through ring
; see https://www.booleanknot.com/blog/2016/07/15/asynchronous-ring.html
(extend-type clojure.core.async.impl.channels.ManyToManyChannel
  protocols/StreamableResponseBody
  (write-body-to-stream [ch _ ^java.io.OutputStream output-stream]
    (go
      ; open a writer to an io output stream
      (with-open [writer (io/writer output-stream)]
        (loop []
          ; wait foa a message on the channel
          (when-let [msg (<! ch)]
            ; write the message to the writer
            (doto writer
              (.write msg)
              (.flush))
            ; loop back
            (recur)))))))
