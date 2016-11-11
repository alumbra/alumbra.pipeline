(ns alumbra.ring.pipeline.parse-document
  (:require [alumbra.ring.pipeline.core :refer [done!]]))

(defn parse-document
  [{:keys [parser-fn]} {:keys [query] :as state}]
  (let [document (parser-fn query)]
    (if (map? document)
      (assoc state :document document)
      (done! {:status 400, :body document}))))
