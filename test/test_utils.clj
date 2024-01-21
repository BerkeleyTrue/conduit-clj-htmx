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
        ; hash map and array map should be treated as the same
        (and (= actual-class clojure.lang.PersistentArrayMap)
             (= expected-class clojure.lang.PersistentHashMap)) clojure.lang.PersistentArrayMap
        (and (= actual-class clojure.lang.PersistentHashMap)
             (= expected-class clojure.lang.PersistentArrayMap)) clojure.lang.PersistentArrayMap
        (not= actual-class expected-class) :no-match
        :else actual-class))))


(defmethod to-equal nil
  [actual expected]
  (do-report {:type :fail :expected expected :actual actual :message "Values are nil"})
  false)

(defmethod to-equal :no-match
  [actual expected]
  (do-report {:type :fail
              :expected expected
              :actual actual
              :message (str "Classes do not match. Expected: " (class expected) " Actual: " (class actual))})
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
