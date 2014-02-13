(ns method.fn-test
  (:require [clojure.test :refer :all]
            [method.fn :as mf]))

(def expected
  ["foo" "bar" "baz"])

(def words
  [" foo" " bar " "baz "])

(deftest test-instance
  (is (= expected (map #mf/i trim words)))
  (is (= expected (map (mf/i trim) words)))
  (is (= expected (map #mf/i String/trim words)))
  (is (= expected (map (mf/i String/trim) words))))

(deftest test-static
  (is (= expected (map #mf/s String/valueOf expected)))
  (is (= expected (map (mf/s String/valueOf) expected)))
  (is (= [1 2 4 8 16] (map (comp long #mf/s Math/pow)
                           (repeat 2) (range 5)))))

(deftest test-constructor
  (is (= (map biginteger (range 5))
         (map (comp #mf/c BigInteger str) (range 5))))
  (is (= (map biginteger (range 5))
         (map (comp (mf/c BigInteger) str) (range 5)))))

(deftest test-caching
  (is (identical? (class #mf/i String/trim)
                  (class #mf/i String/trim)))
  (is (identical? (class #mf/s Math/pow)
                  (class #mf/s Math/pow))))
