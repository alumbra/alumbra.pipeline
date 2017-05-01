(ns alumbra.pipeline.steps.parse-document
  (:require [alumbra.pipeline.steps.core :as pipeline]))

(defn parse-document
  [{:keys [parser-fn]} {:keys [query] :as state}]
  (let [document (parser-fn query)]
    (if (:alumbra/parser-errors document)
      (pipeline/failure!
        (assoc state :errors document)
        :parser-error)
      (assoc state :document document))))
