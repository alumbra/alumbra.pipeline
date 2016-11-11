(ns alumbra.ring.pipeline.execute-operation)

(defn execute-operation
  [{:keys [executor-fn]} {:keys [context canonical-operation]}]
  (executor-fn context canonical-operation))
