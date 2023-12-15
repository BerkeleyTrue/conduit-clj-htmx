(ns conduit.core
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [conduit.server :as server]
   [conduit.config :refer [config]]))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(defonce system (atom nil))

(defn stop-app []
  (some-> (deref system) (ig/halt!))
  (shutdown-agents))

(defn start-app [& _]
  (->> (:server config)
       (ig/prep)
       (ig/init)
       (reset! system))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& _]
  (start-app))
