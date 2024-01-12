(ns conduit.timbre
  (:require
   [taoensso.timbre :as timbre])
  (:import
   [java.time ZoneId]))

(timbre/merge-config!
 {:timestamp-opts {:pattern "HH:mm:ss"
                   :locale (java.util.Locale. "en")
                   :time-zone (ZoneId/systemDefault)}})

