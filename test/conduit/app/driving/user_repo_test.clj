(ns conduit.app.driving.user-repo-test
  (:require
   [clojure.test :refer [deftest testing is] :as t]
   [fixtures.use-node :refer [db-fixture *node*]]
   [conduit.app.driving.user-repo :as user-repo]
   [test-utils :refer [to-equal]])
  (:import [java.util UUID]))

(t/use-fixtures :each db-fixture)

(deftest test-user-repo
  (testing "user-repo"
    (testing "creating a user"
      (let [id (UUID/randomUUID)]
        (is
          (to-equal
            ((user-repo/->create-user *node*)
             {:id id
              :email "foo@bar.com"
              :password "password"
              :created-at "2020-01-01"})
            {:user/id id
             :user/email "foo@bar.com"}))))

    (testing "getting a user by email"
      (is
        (to-equal
          (do
            ((user-repo/->create-user *node*)
             {:id :user/id2
              :email "bar@baz.com"
              :password "password"
              :created-at "2020-01-01"})
            ((user-repo/->get-by-email *node*)
             {:email "bar@baz.com"}))
          {:user/id :user/id2
           :user/email "bar@baz.com"})))

    (testing "getting a user by username"
      (is
        (to-equal
          (do
            ((user-repo/->create-user *node*)
             {:id :user/id3
              :username "bar"
              :password "password"
              :created-at "2020-01-01"})
            ((user-repo/->get-by-email *node*)
             {:email "bar@baz.com"}))
          {:user/id :user/id3
           :user/username "bar"})))))
