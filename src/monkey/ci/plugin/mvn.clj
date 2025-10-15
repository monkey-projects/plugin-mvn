(ns monkey.ci.plugin.mvn
  "Provides MonkeyCI jobs for running maven in build scripts"
  (:require [clojure.string :as cs]
            [monkey.ci.api :as m]))

(def default-img "docker.io/maven:3.9.9-eclipse-temurin-23-alpine")

(def default-m2-cache
  "Location of the m2 cache"
  ".m2")

(def default-release-regex #".+")
(def default-test-job-id "mvn-test")
(def default-verify-job-id "mvn-verify")
(def default-deploy-job-id "mvn-deploy")

(defn- cache-opt [dir]
  (str "-Dmaven.repo.local=" dir))

(defn- add-cache-opt [opts m2-cache]
  (cond-> (or opts [])
    m2-cache (concat [(cache-opt m2-cache)])))

(defn format-cmd
  "Formats the command line for maven with given goals and additional options"
  [{:keys [goals opts m2-cache]
    :or {m2-cache default-m2-cache}}]
  (->> (concat ["mvn"] (add-cache-opt opts m2-cache) goals)
       (cs/join " ")))

(defn mvn
  "Creates mvn container job, with default image that executes the specified command
   with any additional options and args."
  [{:keys [job-id cmd m2-cache]
    :or {job-id "mvn"
         m2-cache default-m2-cache}
    :as conf}]
  (-> (m/container-job job-id)
      (m/image default-img)
      (m/script [(if cmd
                   (format "mvn %s %s" (cache-opt m2-cache) cmd)
                   (format-cmd conf))])
      (m/caches [(m/cache "mvn:m2-cache" m2-cache)])))

(def surefire-reports (m/artifact "surefire-reports"
                                  "target/surefire-reports/"))

(defn- surefire-producing-job [opts]
  (-> (mvn opts)
      (m/save-artifacts [surefire-reports])))

(defn verify
  "Creates `mvn verify` job, with default id `verify`"
  [& [id]]
  (surefire-producing-job {:job-id (or id default-verify-job-id)
                           :cmd "verify"}))

(defn test
  "Creates `mvn test` job, with default id `test`"
  [& [id]]
  (surefire-producing-job {:job-id (or id default-test-job-id)
                           :cmd "test"}))

(defn release? [ctx {:keys [release-regex]
                     :or {release-regex default-release-regex}}]
  (when-let [t (m/tag ctx)]
    (some? (re-matches release-regex t))))

(defn deploy
  "When on main branch, creates a `mvn deploy:deploy` job"
  [& [{:keys [verify-job-id
              job-id]
       :or {verify-job-id default-verify-job-id
            job-id default-deploy-job-id}
       :as conf}]]
  (fn [ctx]
    ;; TODO settings.xml from params for deployment credentials
    (when (or (m/main-branch? ctx)
              (release? ctx conf))
      (-> (mvn {:job-id job-id
                :cmd "deploy:deploy"})
          (m/depends-on verify-job-id)))))

(defn lib
  "Generates jobs for verifying and publishing a maven-based library, that will only
   publish when built from the main branch."
  [& [conf]]
  (fn [ctx]
    (->> [(verify (:verify-job-id conf))
          ((-> conf
               (assoc :job-id (:deploy-job-id conf))
               (deploy))
           ctx)]
         (remove nil?))))
