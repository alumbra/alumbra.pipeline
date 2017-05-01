(ns alumbra.pipeline.steps.check-body
  (:require [alumbra.pipeline.steps.core :as pipeline]))

(defn check-body
  [opts {{:keys [body]} :request, :as state}]
  (if (and (map? body)
           (string? (get body "query")))
    state
    (pipeline/failure! state :invalid-request-body)))
