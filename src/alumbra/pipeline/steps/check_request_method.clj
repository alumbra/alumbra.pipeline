(ns alumbra.pipeline.steps.check-request-method
  (:require [alumbra.pipeline.steps.core :as pipeline]))

(defn check-request-method
  [_ {{:keys [request-method]} :request, :as state}]
  (if-not (= request-method :post)
    (pipeline/failure! state :invalid-request-method)
    state))
