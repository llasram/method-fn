(ns method.fn-test
  (:require [clojure.test :refer :all]
            [method.fn]))

(def expected
  ["foo" "bar" "baz"])

(def words
  [" foo" " bar " "baz "])

(deftest test-instance
  (is (= expected (map #mf/i trim words)))
  (is (= expected (map #mf/i String/trim words))))

(deftest test-static
  (is (= expected (map #mf/s String/valueOf expected)))
  (is (= [1 2 4 8 16] (map (comp long #mf/s Math/pow)
                           (repeat 2) (range 5)))))

(deftest test-caching
  (is (identical? #mf/i String/trim #mf/i String/trim))
  (is (identical? #mf/s Math/pow #mf/s Math/pow)))
