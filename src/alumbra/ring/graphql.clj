(ns alumbra.ring.graphql
  (:require [alumbra.ring.pipeline
             [core :refer [pipeline->>]]
             [canonicalize-operation :refer [canonicalize-operation]]
             [check-body :refer [check-body]]
             [check-request-method :refer [check-request-method]]
             [execute-operation :refer [execute-operation]]
             [parse-document :refer [parse-document]]
             [validate-document :refer [validate-document]]]
            [alumbra.ring.errors :as errors]
            [ring.middleware.json :as json]))

(defn- handle-graphql-request
  [request opts]
  (pipeline->> request
               (check-request-method)
               (check-body opts)
               (parse-document opts)
               (validate-document opts)
               (canonicalize-operation opts)
               (execute-operation opts)))

(defn- make-graphql-handler
  "Generate a handler compatible with both the classical Ring style and the
   CPS one."
  [opts]
  (let [handler-fn #(handle-graphql-request % opts)]
    (fn
      ([request]
       (handler-fn request))
      ([request respond raise]
       (try
         (respond (handler-fn request))
         (catch Throwable t
           (raise t)))))))

(defn handler
  "Generate a Ring Handler for handling GraphQL requests.

   - `:parserf-`: a parser function for GraphQL documents (producing a value
     conforming to either `:alumbra/document` or `:alumbra/parser-errors`),
   - `:validator-fn`: a function taking an `:alumbra/document` and producing either
     `nil` or a value conforming to `:alumbra/validation-errors`,
   - `:canonicalize-fn`: a function taking an `:alumbra/document` and producing
     an `:alumbra/canonical-document`,
   - `:context-fn`: a function taking an HTTP request and returning a value
     representing the context of the GraphQL query,
   - `:executor-fn`: an executor function, taking the request context, as well as
     a map conforming to `:alumbra/canonical-operation` and returning the resolved
     result.

   The resulting handler will expect queries to be sent using `POST`,
   represented by a JSON map with the keys `\"operationName\"`, `\"query\"`
   and \"variables\"."
  [{:keys [parser-fn validator-fn canonicalize-fn executor-fn context-fn]
    :as opts}]
  {:pre [(fn? parser-fn)
         (fn? validator-fn)
         (fn? canonicalize-fn)
         (fn? executor-fn)]}
  (-> (make-graphql-handler
        {:parser-fn       parser-fn
         :validator-fn    validator-fn
         :canonicalize-fn canonicalize-fn
         :executor-fn     executor-fn
         :context-fn      context-fn})
      (errors/wrap)
      (json/wrap-json-response)
      (json/wrap-json-body)))
