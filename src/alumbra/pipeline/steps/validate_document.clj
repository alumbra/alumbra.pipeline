(ns alumbra.pipeline.steps.validate-document
  (:require [alumbra.pipeline.steps.core :as pipeline]))

(defn validate-document
  [{:keys [validator-fn]}
   {:keys [document variables] :as state}]
  (if-let [errors (validator-fn document variables)]
    (pipeline/failure!
      (assoc state :errors errors)
      :validation-error)
    state))
