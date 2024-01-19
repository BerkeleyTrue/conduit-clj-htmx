(ns conduit.infra.middleware.session.xtdb-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ring.middleware.session.store :refer [read-session write-session delete-session]]
   [fixtures.use-node :refer [db-fixture *node*]]
   [conduit.infra.middleware.session.xtdb :as session]))

(use-fixtures :each db-fixture)

(deftest test-session
  (testing "session"
    (let [store (session/->XtdbStore *node*)]
      (is (= nil (read-session store "foo")))
      (is (= "foo" (write-session store "foo" "bar")))
      (is (= "bar" (read-session store "foo")))
      (is (= nil (delete-session store "foo")))
      (is (= nil (read-session store "foo"))))))
