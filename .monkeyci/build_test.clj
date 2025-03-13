(ns build-test
  (:require [clojure.test :refer [deftest testing is]]
            [build :as sut]
            [monkey.ci.build.v2 :as m]))

(deftest jobs
  (testing "defines two entries"
    (is (= 2 (count sut/jobs)))))
