(ns monkey.ci.plugin.mvn
  "Provides MonkeyCI jobs for running maven in build scripts"
  (:require [monkey.ci.build.v2 :as m]))

(def default-img "docker.io/3.9.9-eclipse-temurin-23-alpine")

(defn mvn
  "Creates mvn container job, with default image that executes the specified command"
  [{:keys [job-id cmd]
    :or {job-id "mvn"}
    :as conf}]
  (-> (m/container-job job-id)
      (m/image default-img)
      (m/script [(str "mvn " cmd)])))

(defn verify
  "Creates `mvn verify` job, with default id `verify`"
  [& [id]]
  (mvn {:job-id (or id "verify")
        :cmd "verify"}))
