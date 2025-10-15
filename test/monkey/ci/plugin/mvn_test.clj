(ns monkey.ci.plugin.mvn-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cs]
            [monkey.ci.plugin.mvn :as sut]
            [monkey.ci.api :as m]
            [monkey.ci.test :as mt]))

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
                       "mvn:m2-cache")))))

  (testing "with goals"
    (testing "invokes maven with goals"
      (is (re-matches #"^mvn .* verify$"
                      (->> (sut/mvn {:goals ["verify"]})
                           (m/script)
                           first))))

    (testing "passes options to maven"
      (is (re-matches #"^mvn -T 10 .* verify$"
                      (->> (sut/mvn {:goals ["verify"]
                                     :opts ["-T 10"]})
                           (m/script)
                           first))))))

(deftest verify-job
  (testing "creates mvn job"
    (let [job (sut/verify "test-verify")]
      (testing "that performs verify command"
        (is (cs/ends-with?
             (first (m/script job))
             " verify")))

      (testing "with specified id"
        (is (= "test-verify"
               (m/job-id job))))))

  (testing "has default id"
    (is (= "mvn-verify"
           (-> (sut/verify)
               (m/job-id)))))

  (testing "saves surefire reports as artifact"
    (let [a (:save-artifacts (sut/verify))]
      (is (= 1 (count a))))))

(deftest test-job
  (testing "creates mvn job"
    (let [job (sut/test "test-test")]
      (testing "that performs test command"
        (is (cs/ends-with?
             (first (m/script job))
             " test")))

      (testing "with specified id"
        (is (= "test-test"
               (m/job-id job))))))

  (testing "has default id"
    (is (= "mvn-test"
           (-> (sut/test)
               (m/job-id)))))

  (testing "saves surefire reports as artifact"
    (let [a (:save-artifacts (sut/test))]
      (is (= 1 (count a))))))

(deftest deploy-job
  (testing "on main branch"
    (let [job (-> mt/test-ctx
                  (mt/with-git-ref "refs/heads/main")
                  ((sut/deploy)))]
      (testing "creates mvn job"
        (is (m/container-job? job)))

      (testing "invokes `deploy:deploy`"
        (is (cs/ends-with? (-> job :script first)
                           "deploy:deploy")))

      (testing "depends on verify job"
        (is (= [sut/default-verify-job-id] (:dependencies job))))))

  (testing "on release tag"
    (let [make-job (sut/deploy {:release-regex #"^v\d+\.\d+.*$"})
          job (-> mt/test-ctx
                  (mt/with-git-ref "refs/tags/v0.1.0")
                  (make-job))]
      (testing "creates mvn job"
        (is (m/container-job? job)))))

  (testing "on non-release tag"
    (let [make-job (sut/deploy {:release-regex #"^v\d+\.\d+.*$"})
          job (-> mt/test-ctx
                  (mt/with-git-ref "refs/tags/other")
                  (make-job))]
      (testing "does not create mvn job"
        (is (nil? job)))))

  (testing "not on main branch"
    (let [job (-> mt/test-ctx
                  ((sut/deploy)))]
      (testing "does not create mvn job"
        (is (nil? job))))))

(deftest lib-jobs
  (testing "when no deploy is required"
    (let [jobs ((sut/lib) mt/test-ctx)]
      (testing "creates verify job"
        (is (= 1 (count jobs)))
        (is (= sut/default-verify-job-id
               (-> jobs first m/job-id))))))

  (testing "on main branch"
    (let [jobs ((sut/lib)
                (-> mt/test-ctx
                    (mt/with-git-ref "refs/heads/main")))]
      (testing "creates verify and deploy jobs"
        (is (= 2 (count jobs))))))

  (testing "can override job ids"
    (let [jobs ((sut/lib {:verify-job-id "test"
                          :deploy-job-id "deploy"})
                (-> mt/test-ctx
                    (mt/with-git-ref "refs/heads/main")))]
      (is (= ["test" "deploy"]
             (map m/job-id jobs))))))
