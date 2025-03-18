(ns monkey.ci.plugin.mvn
  "Provides MonkeyCI jobs for running maven in build scripts"
  (:require [monkey.ci.build.v2 :as m]))

(def default-img "docker.io/maven:3.9.9-eclipse-temurin-23-alpine")

(def default-m2-cache
  "Location of the m2 cache"
  ".m2")

(defn mvn
  "Creates mvn container job, with default image that executes the specified command"
  [{:keys [job-id cmd m2-cache]
    :or {job-id "mvn"
         m2-cache default-m2-cache}
    :as conf}]
  (-> (m/container-job job-id)
      (m/image default-img)
      (m/script [(format "mvn -Dmaven.repo.local=%s %s" m2-cache cmd)])
      (m/caches [(m/cache "m2-cache" m2-cache)])))

(defn verify
  "Creates `mvn verify` job, with default id `verify`"
  [& [id]]
  (-> (mvn {:job-id (or id "verify")
            :cmd "verify"})
      (m/save-artifacts [(m/artifact "surefire-reports"
                                     "target/surefire-reports/")])))
