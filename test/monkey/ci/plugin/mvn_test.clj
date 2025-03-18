(ns monkey.ci.plugin.mvn-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cs]
            [monkey.ci.plugin.mvn :as sut]
            [monkey.ci.build.v2 :as m]))

(deftest mvn
  (let [job (sut/mvn {:cmd "test-cmd"})]
    (testing "creates container job"
      (is (m/container-job? job)))

    (testing "uses default image when not specified"
      (is (= sut/default-img (m/image job))))

    (let [s (m/script job)]
      (testing "invokes mvn with specified command"
        (is (= 1 (count s)))
        (is (re-matches #"^mvn .*test-cmd$" (first s))))

      (testing "specifies m2 cache location"
        (is (cs/includes? (first s) "-Dmaven.repo.local=.m2"))
        (is (contains? (->> job
                            :caches
                            (map :id)
                            (set))
                       "m2-cache"))))))

(deftest verify
  (testing "creates mvn job"
    (let [job (sut/verify "test-verify")]
      (testing "that performs verify command"
        (is (= ["mvn verify"]
               (m/script job))))

      (testing "with specified id"
        (is (= "test-verify"
               (m/job-id job))))))

  (testing "has default id"
    (is (= "verify"
           (-> (sut/verify)
               (m/job-id)))))

  (testing "saves surefire reports as artifact"
    (let [a (:save-artifacts (sut/verify))]
      (is (= 1 (count a))))))
