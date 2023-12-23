(ns conduit.tasks
  (:require
   [babashka.tasks :as tasks :refer [clojure]]
   [clojure.java.io :as io]))

(def config
  {:clj-args ["-J-XX:-OmitStackTraceInFastThrow"
              "-J-XX:+CrashOnOutOfMemoryError"
              "-J-Duser.timezone=UTC"
              "-M:dev"]})

(defn dev
  "Starts the app in dev mode"
  [& args]
  (io/make-parents "target/resources/_")
  (apply clojure (concat args (:clj-args config))))

(comment
  (dev))
