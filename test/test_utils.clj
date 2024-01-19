(ns test-utils
  (:require
    [clojure.test :refer [do-report]]))


(defmulti to-equal
  "Compare two values, with the same class, for equality.
  Only check if values are equal from expected to actual.
  Ignores nil, does not count all keys in actual."
  (fn [actual expected]
    (let [actual-class (class actual)
          expected-class (class expected)]
      (cond
        (or (nil? actual) (nil? expected)) nil
        (not= actual-class expected-class) :no-match
        :else actual-class))))


(defmethod to-equal nil
  [actual expected]
  (do-report {:type :fail :expected expected :actual actual :message "Values are nil"})
  false)

(defmethod to-equal :no-match
  [actual expected]
  (do-report {:type :fail :expected expected :actual actual :message "Classes do not match"})
  false)

(defmethod to-equal clojure.lang.PersistentArrayMap
  [actual expected]
  (let [res (every? (fn [[k v]] (= v (get actual k))) expected)]
    (if res
      (do-report {:type :pass :expected expected :actual actual})
      (do-report {:type :fail :expected expected :actual actual}))
    res))

(comment
  (to-equal {:foo "bar" :baz "quz"} {:foo "bar"})
  (to-equal {:foo "bar" :baz "quz"} {:baz "bar"}))
