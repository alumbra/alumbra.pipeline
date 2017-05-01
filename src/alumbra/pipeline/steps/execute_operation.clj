(ns alumbra.pipeline.steps.execute-operation
  (:require [alumbra.pipeline.steps.core :as pipeline]))

(defn execute-operation
  [{:keys [executor-fn]} {:keys [context canonical-operation] :as state}]
  (let [{:keys [data errors]} (executor-fn context canonical-operation)]
    (if (empty? errors)
      (assoc state :data data)
      (pipeline/failure!
        (assoc state
               :data   data
               :errors {:alumbra/execution-errors errors})
        :execution-error))))
