(ns alumbra.ring.pipeline.read-body
  (:require [alumbra.ring.pipeline.core :refer [done!]]
            [alumbra.ring.errors :as errors]
            [cheshire.core :as json]))

(def ^:private invalid-variables-string-response
  (errors/single-error-response
    400
    (str "If the 'variables' field is given as a string, "
         "it needs to contain valid JSON")))

(def ^:private invalid-variables-response
  (errors/single-error-response
    400
    "The 'variables' field needs to be given as a JSON object."))

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

(defn read-body
  [_ {{:keys [body]} :request, :as state}]
  (let [{:strs [operationName query variables]} body]
    (-> state
        (merge
          {:operation-name operationName
           :query          query})
        (add-variables variables))))
