(ns monkey.ci.plugin.mvn-test
  (:require [clojure.test :refer [deftest testing is]]
            [monkey.ci.plugin.mvn :as sut]
            [monkey.ci.build.v2 :as m]))

(deftest mvn
  (let [job (sut/mvn {:cmd "test-cmd"})]
    (testing "creates container job"
      (is (m/container-job? job)))

    (testing "uses default image when not specified"
      (is (= sut/default-img (m/image job))))

    (testing "invokes mvn with specified command"
      (is (= ["mvn test-cmd"]
             (m/script job))))))

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
               (m/job-id))))))
