(ns alumbra.ring.pipeline.check-body
  (:require [alumbra.ring.errors :as errors]
            [alumbra.ring.pipeline.core :refer [done!]]
            [cheshire.core :as json]))

;; ## Responses

(def ^:private invalid-body-response
  (errors/single-error-response
    400
    "The HTTP body needs to be a JSON map with at least the 'query' field!"))

(def ^:private invalid-variables-string-response
  (errors/single-error-response
    400
    (str "If the 'variables' field is given as a string, "
         "it needs to contain valid JSON")))

(def ^:private invalid-variables-response
  (errors/single-error-response
    400
    "The 'variables' field needs to be given as a JSON object."))

;; ## Logic

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
            (done! invalid-variables-string-response)))

        (map? variables)
        (assoc state :variables variables)

        :else
        (done! invalid-variables-response)))

(defn- prepare-request
  [{:keys [context-fn]} {:keys [body] :as request}]
  (let [{:strs [operationName query variables]} body]
    (-> {:context        (if context-fn (context-fn request) {})
         :operation-name operationName
         :query          query}
        (add-variables variables))))

(defn check-body
  [opts {:keys [body] :as request}]
  (if-not (and (some-> body map)
               (string? (get body "query")))
    (done! invalid-body-response)
    (prepare-request opts request)))
