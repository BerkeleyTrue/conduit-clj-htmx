(ns conduit.app.drivers.hot-reload
  (:require
   [clojure.core.async :refer [go chan <! >! close!]]
   [ring.util.response :as util]
   [hiccup.util :refer [raw-string]]
   [conduit.infra.hiccup :refer [defhtml]]))

(defhtml hot-reload-script []
  [:script {:type "text/javascript"}
   (raw-string "
      function initHotReload() {
        if (typeof EventSource !== 'undefined') {

          var source = new EventSource('/__hotreload');

          source.onmessage = function(event) {
            if (event.data === 'updated') {
              console.log('hotreload: updated');
              source.close();
              setTimeout(() => {
                window.location.reload();
              }, 500);
            } else if (event.data === 'connected') {
              console.log('hotreload: connected');
            } else {
              console.log('hotreload: unknown event', event);
            }
          };

          source.onerror = function(event) {
            console.log('hotreload: err', event.message);
            source.close();
            setTimeout(initHotReload, 1000);
          };
          window.onbeforeunload = function() {
            source.close();
          };
        } else {
          console.log('Your browser does not support server-sent events...');
        }
      }

      setTimeout(initHotReload, 1000);
   ")])

(comment
  (hot-reload-script))

(defn ->get-sse [on-start-ch]
  (fn get-sse [_ response _]
    (let [out (chan)]
      (->
       out
       (util/response)
       (util/content-type "text/event-stream")
       (util/header "Cache-Control" "no-cache")
       (util/header "Connection" "keep-alive")
       (response))
      (go
        (println "SSE: connection established")
        (>! out "data: connected\n\n")
        ; wait for the on-start-ch
        (<! on-start-ch)
        (>! out "data: updated\n\n")
        (println "SSE: updated")
        (close! out)))))
