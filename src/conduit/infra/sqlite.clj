(ns conduit.infra.sqlite
  (:require
   [integrant.core :as ig]
   [next.jdbc :as jdbc]
   [taoensso.timbre :as timbre]
   [hugsql.core :as hugsql]
   [hugsql.adapter.next-jdbc :as adapter]))

(hugsql/set-adapter! (adapter/hugsql-adapter-next-jdbc))

(defmethod ig/init-key :infra.db/jdbc [_ {:keys [db]}]
  (timbre/info "init ds")
  (jdbc/get-datasource {:dbtype "sqlite" :dbname db}))
