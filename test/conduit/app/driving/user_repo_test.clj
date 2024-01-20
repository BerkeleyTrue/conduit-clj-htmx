(ns conduit.app.driving.user-repo-test
  (:require
   [clojure.test :refer [deftest testing is] :as t]
   [xtdb.api :as xt]
   [fixtures.use-node :refer [->db-fixture *node*]]
   [conduit.app.driving.user-repo :as user-repo]
   [test-utils :refer [to-equal]])
  (:import [java.util UUID]))

(t/use-fixtures :each (->db-fixture user-repo/transaction-functions))

(deftest user-repo-create
  (testing "creating a user"
    (let [id (UUID/randomUUID)]
      (is
       (to-equal
        ((user-repo/->create-user *node*)
         {:id id
          :email "foo@bar.com"
          :password "password"
          :created-at "2020-01-01"})
        {:xt/id id
         :user/email "foo@bar.com"})))))

(deftest user-repo-get-by
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
      {:xt/id :user/id2
       :user/email "bar@baz.com"}))

    (testing "getting a user by username"
      (is
       (to-equal
        (do
          ((user-repo/->create-user *node*)
           {:id :user/id3
            :email "bar@baz2.com"
            :username "bar"
            :password "password"
            :created-at "2020-01-01"})
          ((user-repo/->get-by-username *node*)
           {:username "bar"}))
        {:xt/id :user/id3
         :user/username "bar"})))))

(deftest user-repo-follow
  (testing "following an author"
    (let [create-user (user-repo/->create-user *node*)
          follow-author (user-repo/->follow *node*)]
      (create-user
        {:id :user/id4
         :email "foo@bar.com"
         :username "foo"})
      (create-user
        {:id :user/id5
         :email "foo2@bar.com"
         :username "foo2"})
      (create-user
        {:id :user/id6
         :email "bar@bar.com"
         :username "bar"})
      (follow-author
        {:author-id :user/id4
         :user-id :user/id5})
      (xt/sync *node*)
      (is
        (to-equal
          ((user-repo/->get-by-username *node*)
           {:username "foo2"})
          {:xt/id :user/id5
           :user/username "foo2"
           :user/following #{:user/id4}}))
      (follow-author
        {:author-id :user/id6
         :user-id :user/id5})
      (is
        (to-equal
          ((user-repo/->get-by-username *node*)
           {:username "foo2"})
          {:xt/id :user/id5
           :user/username "foo2"
           :user/following #{:user/id4 :user/id6}}))

      (testing "unfollowing an author"
        (let [unfollow (user-repo/->unfollow *node*)]
          (unfollow {:author-id :user/id4
                     :user-id :user/id5})
          (xt/sync *node*)
          (is
            (to-equal
              ((user-repo/->get-by-username *node*)
               {:username "foo2"})
              {:xt/id :user/id5
               :user/username "foo2"
               :user/following #{:user/id6}})))))))

(deftest user-repo-update
  (testing "updating a user"
    (let [create-user (user-repo/->create-user *node*)
          update-user (user-repo/->update *node*)]
      (create-user
        {:id :user/id7
         :email "foo@bar.com"})
      (is
        (to-equal
          ((user-repo/->get-by-email *node*)
           {:email "foo@bar.com"})
          {:xt/id :user/id7
           :user/email "foo@bar.com"})
        "to update create user")
      (is
        (to-equal
          (update-user
            {:id :user/id7
             :email "foo@bar.com"
             :image "foo/bar/baz.png"})
          {:xt/id :user/id7
           :user/email "foo@bar.com"
           :user/image "foo/bar/baz.png"})
        "to add image and update email")
      (is
        (to-equal
          (update-user
            {:id :user/id7
             :email "foo2@bar2.com"
             :username "new"
             :image "foo/bar/baz2.png"
             :bio "foo bar baz"})
          {:xt/id :user/id7
           :user/email "foo2@bar2.com"
           :user/image "foo/bar/baz2.png"
           :user/bio "foo bar baz"
           :user/username "new"})))))
