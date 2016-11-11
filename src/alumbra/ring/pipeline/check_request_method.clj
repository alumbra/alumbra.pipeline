(ns alumbra.ring.pipeline.check-request-method
  (:require [alumbra.ring.errors :as errors]
            [alumbra.ring.pipeline.core :refer [done!]]))

(def ^:private invalid-request-method-response
  (errors/single-error-response
    405
    "Endpoint needs to be accesed using HTTP POST!"))

(defn check-request-method
  [{:keys [request-method] :as request}]
  (if-not (= request-method :post)
    (done! invalid-request-method-response)
    request))
