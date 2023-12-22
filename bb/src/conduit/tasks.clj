(ns conduit.tasks
  (:require
   [aero.core :as aero]
   [babashka.tasks :as tasks :refer [clojure]]
   [clojure.java.io :as io]))

(def config
  (delay (aero/read-config "tasks.edn")))

(defn dev
  "Starts the app in dev mode"
  [& args]
  (io/make-parents "target/resources/_")
  (apply clojure (concat args (:clj-args @config))))

(comment
  (dev))
