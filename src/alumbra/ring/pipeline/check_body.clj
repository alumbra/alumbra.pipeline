(ns alumbra.ring.pipeline.check-body
  (:require [alumbra.ring.errors :as errors]
            [alumbra.ring.pipeline.core :refer [done!]]))

(def ^:private invalid-body-response
  (errors/single-error-response
    400
    "The HTTP body needs to be a JSON map with at least the 'query' field!"))

(defn- prepare-request
  [{:keys [context-fn]} {:keys [body] :as request}]
  (let [{:strs [operationName query variables]} body]
    {:context        (if context-fn (context-fn request) {})
     :operation-name operationName
     :query          query
     :variables      (into {} variables)}))

(defn check-body
  [opts {:keys [body] :as request}]
  (if-not (and (some-> body map)
               (string? (get body "query")))
    (done! invalid-body-response)
    (prepare-request opts request)))
