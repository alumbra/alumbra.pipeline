(ns alumbra.ring.pipeline.execute-operation
  (:require [alumbra.ring.errors :as errors]))

(defn execute-operation
  [{:keys [executor-fn]} {:keys [context canonical-operation]}]
  (let [{:keys [errors] :as result} (executor-fn context canonical-operation)
        errors? (seq errors)
        status  (if errors? 500 200)
        body    (cond-> result (not errors?) (dissoc :errors))]
    {:status status
     :body   body}))
