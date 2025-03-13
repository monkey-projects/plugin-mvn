(ns build
  (:require [monkey.ci.plugin
             [clj :as clj]
             [github :as gh]]))

(def jobs
  [(clj/deps-library)
   (gh/release-job {:dependencies ["publish"]})])
