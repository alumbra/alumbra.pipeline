(ns alumbra.pipeline.steps
  (:require [alumbra.pipeline.steps
             [core :as pipeline]
             [canonicalize-operation :refer [canonicalize-operation]]
             [check-body :refer [check-body]]
             [check-request-method :refer [check-request-method]]
             [execute-operation :refer [execute-operation]]
             [parse-document :refer [parse-document]]
             [read-body :refer [read-body]]
             [set-request-context :refer [set-request-context]]
             [validate-document :refer [validate-document]]]))

;; ## Raw Pipeline

(defn- as-pipeline-result
  [value]
  (if-let [e (pipeline/error value)]
    (merge
      (if (instance? Throwable e)
        {:status    :exception
         :exception e}
        {:status :error
         :error  e})
      (pipeline/value value))
    (merge
      {:status :success}
      (pipeline/value value))))

(defn- run*
  [opts state]
  (pipeline/pipeline->> state
                          (parse-document opts)
                          (validate-document opts)
                          (canonicalize-operation opts)
                          (execute-operation opts)))

(defn run
  "Runs the given `state` through the pipeline. It has to be a map of:

   - `:query`:          the GraphQL query string,
   - `:operation-name`: the GraphQL operation to run (if multiple are given),
   - `:variables`:      a map of variable names and values,
   - `:context`:        a map describing the context of the operation (e.g. session information).

   This returns an execution result consisting of the input state, plus:

   - `:status`: either `:success`, `:exception` or `:error`.
   - `:data`: the execution result data (if available),
   - `:exception`: a potentially thrown exception,
   - `:error`: a keyword describing the kind of error encountered,
   - `:errors`: a map describing the encountered errors.

   `opts` has to be a map of alumbra.spec-compliant components:

   - `:parser-fn`
   - `:validator-fn`
   - `:canonicalize-fn`
   - `:executor-fn`

   "
  [opts state]
  (as-pipeline-result (run* opts state)))

;; ## Request Pipeline

(defn run-ring-request
  "Like [[run]] but uses the given Ring `request` to extract `:query`,
   `:operation-name` and `:variables`.

   Additionally, `opts` may contain a `:context-fn` key that will be applied
   to the request to derive the `:context` key.

   Note that the request's `:body` has to be a map (i.e. already JSON-decoded)."
  [opts request]
  (as-pipeline-result
    (pipeline/pipeline->> {:request request}
                          (check-request-method opts)
                          (check-body opts)
                          (read-body opts)
                          (set-request-context opts)
                          (run* opts))))
