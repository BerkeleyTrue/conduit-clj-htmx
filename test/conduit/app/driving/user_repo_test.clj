(ns conduit.app.driving.user-repo-test
  (:require
   [clojure.test :refer [deftest testing is] :as t]
   [xtdb.api :as xt]
   [fixtures.use-node :refer [->db-fixture *node*]]
   [conduit.app.driving.user-repo :as user-repo]
   [test-utils :refer [to-equal]])
  (:import [java.util UUID]))

(t/use-fixtures :each (->db-fixture user-repo/transaction-functions))

(def id1 (UUID/randomUUID))
(def id2 (UUID/randomUUID))
(def id3 (UUID/randomUUID))
(def id4 (UUID/randomUUID))
(def id5 (UUID/randomUUID))
(def id6 (UUID/randomUUID))

(deftest user-repo-to-domain
  (testing "formatting a user entity to a domain user"
    (let [user-id (UUID/randomUUID)
          uuid1 (UUID/randomUUID)]
      (is
       (to-equal
        (user-repo/format-to-user
         {:xt/id user-id
          :user/email "foo@bar.com"
          :user/username "foo"
          :user/bio "foo bar baz"
          :user/image "foo/bar/baz.png"
          :user/password "password"
          :user/created-at "2020-01-01"
          :user/following #{uuid1}})

        {:user-id user-id
         :username "foo"
         :bio "foo bar baz"
         :image "foo/bar/baz.png"
         :following #{uuid1}
         :password "password"
         :created-at "2020-01-01"
         :updated-at nil}))
      (is
       (to-equal
        (user-repo/format-to-user
         {:xt/id user-id
          :user/email "foo@bar.com"
          :user/username "foo"
          :user/password "password"
          :user/created-at "2020-01-01"
          :user/following #{uuid1}})

        {:user-id user-id
         :username "foo"
         :following #{uuid1}
         :password "password"
         :created-at "2020-01-01"
         :updated-at nil}))
      (is
       (=
        (user-repo/format-to-user nil)
        nil)))))

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
        {:user-id id
         :email "foo@bar.com"})))))

(deftest user-repo-get-by
  (testing "getting a user by email"
    (is
     (to-equal
      (do
        ((user-repo/->create-user *node*)
         {:id id2
          :email "bar@baz.com"
          :password "password"
          :created-at "2020-01-01"})
        ((user-repo/->get-by-email *node*)
         {:email "bar@baz.com"}))
      {:user-id id2
       :email "bar@baz.com"}))

    (testing "getting a user by username"
      (is
       (to-equal
        (do
          ((user-repo/->create-user *node*)
           {:id id3
            :email "bar@baz2.com"
            :username "bar"
            :password "password"
            :created-at "2020-01-01"})
          ((user-repo/->get-by-username *node*)
           {:username "bar"}))
        {:user-id id3
         :username "bar"})))))

(deftest user-repo-follow
  (testing "following an author"
    (let [create-user (user-repo/->create-user *node*)
          follow-author (user-repo/->follow *node*)]
      (create-user
       {:id id4
        :email "foo@bar.com"
        :password ""
        :username "foo"})
      (create-user
       {:id id5
        :password ""
        :email "foo2@bar.com"
        :username "foo2"})
      (create-user
       {:id id6
        :password ""
        :email "bar@bar.com"
        :username "bar"})
      (follow-author
       {:author-id id4
        :user-id id5})
      (xt/sync *node*)
      (is
       (to-equal
        ((user-repo/->get-by-username *node*)
         {:username "foo2"})
        {:user-id id5
         :username "foo2"
         :following #{id4}}))
      (follow-author
       {:author-id id6
        :user-id id5})
      (is
       (to-equal
        ((user-repo/->get-by-username *node*)
         {:username "foo2"})
        {:user-id id5
         :username "foo2"
         :following #{id4 id6}}))

      (testing "unfollowing an author"
        (let [unfollow (user-repo/->unfollow *node*)]
          (unfollow {:author-id id4
                     :user-id id5})
          (xt/sync *node*)
          (is
           (to-equal
            ((user-repo/->get-by-username *node*)
             {:username "foo2"})
            {:user-id id5
             :username "foo2"
             :following #{id6}})))))))

(deftest user-repo-update
  (testing "updating a user"
    (let [create-user (user-repo/->create-user *node*)
          update-user (user-repo/->update *node*)]
      (create-user
       {:id id6
        :email "foo@bar.com"})
      (is
       (to-equal
        ((user-repo/->get-by-email *node*)
         {:email "foo@bar.com"})
        {:user-id id6
         :email "foo@bar.com"})
       "to update create user")
      (is
       (to-equal
        (update-user
         {:id id6
          :email "foo@bar.com"
          :image "foo/bar/baz.png"})
        {:user-id id6
         :email "foo@bar.com"
         :image "foo/bar/baz.png"})
       "to add image and update email")
      (is
       (to-equal
        (update-user
         {:id id6
          :email "foo2@bar2.com"
          :username "new"
          :image "foo/bar/baz2.png"
          :bio "foo bar baz"})
        {:user-id id6
         :email "foo2@bar2.com"
         :image "foo/bar/baz2.png"
         :bio "foo bar baz"
         :username "new"})))))
