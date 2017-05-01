(ns alumbra.pipeline.steps.read-body
  (:require [alumbra.pipeline.steps.core :as pipeline]
            [cheshire.core :as json]))

(defn add-variables
  [state variables]
  (cond (nil? variables)
        (assoc state :variables {})

        (string? variables)
        (let [[success? value] (try
                                 [true (json/parse-string variables)]
                                 (catch Throwable t
                                   [false t]))]
          (if success?
            (recur state value)
            (pipeline/failure! state :invalid-variables-json)))

        (map? variables)
        (assoc state :variables variables)

        :else
        (pipeline/failure! state :invalid-variables)))

(defn read-body
  [_ {{:keys [body]} :request, :as state}]
  (let [{:strs [operationName query variables]} body]
    (-> state
        (merge
          {:operation-name operationName
           :query          query})
        (add-variables variables))))
