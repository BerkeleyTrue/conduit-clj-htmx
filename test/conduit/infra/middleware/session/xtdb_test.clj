(ns conduit.infra.middleware.session.xtdb-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [xtdb.api :as xt]
   [ring.middleware.session.store :refer [read-session write-session delete-session]]
   [conduit.infra.middleware.session.xtdb :as session]))

(def ^:dynamic *node* nil)

(defn db-fixture [f]
  (let [node (xt/start-node {})]
    (binding [*node* node]
      (try
        (f)
        (finally
          (.close node))))
    (.close node)))

(use-fixtures :each db-fixture)

(deftest test-session
  (testing "session"
    (let [store (session/->XtdbStore *node*)]
      (is (= nil (read-session store "foo")))
      (is (= "foo" (write-session store "foo" "bar")))
      (is (= "bar" (read-session store "foo")))
      (is (= nil (delete-session store "foo")))
      (is (= nil (read-session store "foo"))))))
