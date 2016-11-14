(ns alumbra.ring.pipeline.execute-operation
  (:require [alumbra.ring.errors :as errors]))

(defn execute-operation
  [{:keys [executor-fn]} {:keys [context canonical-operation]}]
  (try
    (let [result (executor-fn context canonical-operation)]
      {:status 200
       :body   result})
    (catch Throwable t
      ;; TODO Logging
      (.printStackTrace t)
      (errors/single-error-response
        500
        (str "An unexpected error occured during execution:\n"
             (.getMessage t))))))
