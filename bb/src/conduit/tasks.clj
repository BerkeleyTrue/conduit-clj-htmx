(ns conduit.tasks
  (:require
   [babashka.tasks :as tasks :refer [clojure]]
   [clojure.java.io :as io]
   [aero.core :as aero]))

(def config
  (delay (:tasks (aero/read-config "config.edn"))))

(defn read-args []
  (:clj-args @config))

(defn dev
  "Starts the app in dev mode"
  [& args]
  (io/make-parents "target/resources/_")
  (spit ".nrepl-port" "7888")
  (apply clojure (concat args (read-args))))

(comment (dev))
