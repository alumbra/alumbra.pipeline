(ns alumbra.ring.pipeline.check-body
  (:require [alumbra.ring.errors :as errors]
            [alumbra.ring.pipeline.core :refer [done!]]))

(def ^:private invalid-body-response
  (errors/single-error-response
    400
    "The HTTP body needs to be a JSON map with at least the 'query' field!"))

(defn check-body
  [opts {{:keys [body]} :request, :as state}]
  (if-not (and (some-> body map)
               (string? (get body "query")))
    (done! invalid-body-response)
    state))
