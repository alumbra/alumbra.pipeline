(ns alumbra.ring.pipeline.check-body
  (:require [alumbra.ring.errors :as errors]
            [alumbra.ring.pipeline.core :refer [done!]]))

(def ^:private invalid-body-response
  (errors/single-error-response
    400
    (str "The HTTP body needs to be a JSON map of \"operationName\" (string), "
         "\"query\" (string) and \"variables\" (object)!")))

(defn- prepare-request
  [{:keys [context-fn]} {:keys [body] :as request}]
  (let [{:strs [operationName query variables]} body]
    (if (and (or (nil? operationName) (string? operationName))
             (or (nil? variables) (map? variables))
             (string? query))
      {:context        (if context-fn (context-fn request) {})
       :operation-name operationName
       :query          query
       :variables      variables}
      (done! invalid-body-response))))

(defn check-body
  [opts {:keys [body] :as request}]
  (if-not (and (some-> body map)
               (every? #(contains? body %)
                       ["operationName" "query" "variables"]))
    (done! invalid-body-response)
    (prepare-request opts request)))
