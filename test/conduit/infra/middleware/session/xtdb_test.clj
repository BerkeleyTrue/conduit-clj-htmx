(ns conduit.infra.middleware.session.xtdb-test
  (:require
   [clojure.test :refer [deftest is testing]]))

(deftest test-session
  (testing "session"
    (is (= 1 1))))
