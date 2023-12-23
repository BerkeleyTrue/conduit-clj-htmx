(ns conduit.tasks
  (:require
   [babashka.tasks :as tasks :refer [shell]]
   [clojure.java.io :as io]))

(defn dev
  "Starts the app in dev mode"
  []
  (io/make-parents "target/resources/_")
  (shell "clj -M:dev"))

(comment
  (dev))
