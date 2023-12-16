(ns conduit.env
  (:require
   [taoensso.timbre :refer [info]]))

(def defaults
  {:init
   (fn []
     (info "Starting system..."))})

