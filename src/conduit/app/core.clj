(ns conduit.app.core
  (:require
   [clojure.core.async :refer [chan]]
   [integrant.core :as ig]
   [conduit.app.drivers.core]
   [conduit.app.driving.core]))

(defmethod ig/init-key :app/on-start-ch [_ _]
  (chan 1))
