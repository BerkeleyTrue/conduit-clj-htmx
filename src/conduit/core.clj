(ns conduit.core
  (:require
   [taoensso.timbre :refer [error]]
   [integrant.core :as ig]
   [conduit.infra.core]
   [conduit.app.core]
   [conduit.config :refer [config]]))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (error
       {:what :uncaught-exception
        :exception ex
        :where (str "Uncaught exception on" (.getName thread))}))))

(defonce system (atom nil))

(defn stop-app []
  (some->
    (deref system)
    (ig/halt!))
  (shutdown-agents))

(defn start-app [& _]
  (->>
    config
       (ig/prep)
       (ig/init)
       (reset! system))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& _]
  (start-app))
