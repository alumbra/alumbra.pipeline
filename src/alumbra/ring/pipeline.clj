(ns alumbra.ring.pipeline
  (:require [alumbra.ring.pipeline
             [core :refer [pipeline->>]]
             [canonicalize-operation :refer [canonicalize-operation]]
             [check-body :refer [check-body]]
             [check-request-method :refer [check-request-method]]
             [execute-operation :refer [execute-operation]]
             [parse-document :refer [parse-document]]
             [read-body :refer [read-body]]
             [set-request-context :refer [set-request-context]]
             [validate-document :refer [validate-document]]]
            [alumbra.ring.errors :as errors]
            [ring.middleware.json :as json]
            [clojure.walk :refer [keywordize-keys]]))

;; ## Run Function

(defn- run-state
  [opts state]
  (pipeline->> state
               (parse-document opts)
               (validate-document opts)
               (canonicalize-operation opts)
               (execute-operation opts)))

;; ## Request Runner

(defn run-request
  "Run the given Ring request. `opts` needs to contain the following keys:

   - `:parser-fn`
   - `:validator-fn`
   - `:canonicalize-fn`
   - `:executor-fn`
   - `:context-fn`

   A Ring response will be returned."
  [opts request]
  (pipeline->> {:request request}
               (check-request-method opts)
               (check-body opts)
               (read-body opts)
               (set-request-context opts)
               (run-state opts)))

;; ## Query Runner

(defn run-query
  "Run the given GraphQL query string. See [[run-request]] for the necessary
   `opts`. Note that, instead of `:context-fn`, the `:context` key has to
   be given directly.

   Returns only the Ring response body, with keywordized map keys."
  [opts query
   & [{:keys [operation-name variables context]
       :or {variables {}
            context   {}}}]]
  (pipeline->> {:query          query
                :operation-name operation-name
                :variables      variables
                :context        context}
               (run-state opts)
               (:body)
               (keywordize-keys)))
