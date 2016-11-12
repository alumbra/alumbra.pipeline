(ns alumbra.ring.pipeline.parse-document
  (:require [alumbra.ring.pipeline.core :refer [done!]]
            [alumbra.ring.errors :as errors]))

(defn parse-document
  [{:keys [parser-fn]} {:keys [query] :as state}]
  (let [document (parser-fn query)]
    (if-let [errors (:alumbra/parser-errors document)]
      (done! (errors/parser-error-response errors))
      (assoc state :document document))))
