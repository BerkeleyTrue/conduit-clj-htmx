(ns conduit.utils.hyper
  (:require
   [hiccup.util :as util]))

(defn hyper
  [& body]
  (util/raw-string (apply str body)))
