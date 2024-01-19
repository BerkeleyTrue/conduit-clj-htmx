(ns test-utils)


(defmulti to-equal
  "Compare two values, with the same class, for equality.
  Only check if values are equal from expected to actual.
  Ignores nil, does not count all keys in actual."
  (fn [actual expected] [(class actual) (class expected)]))

(defmethod to-equal [clojure.lang.PersistentArrayMap clojure.lang.PersistentArrayMap]
  [actual expected]
  (every? (fn [[k v]] (= v (get actual k))) expected))

(comment
  (to-equal {:foo "bar" :baz "quz"} {:foo "bar"})
  (to-equal {:foo "bar" :baz "quz"} {:baz "bar"}))
