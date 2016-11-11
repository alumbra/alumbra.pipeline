(ns alumbra.ring.handler
  (:require [alumbra.ring.pipeline
             [core :refer [pipeline->>]]
             [canonicalize-operation :refer [canonicalize-operation]]
             [check-body :refer [check-body]]
             [check-request-method :refer [check-request-method]]
             [execute-operation :refer [execute-operation]]
             [parse-document :refer [parse-document]]
             [validate-document :refer [validate-document]]]
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

(defn raw-handler
  "Generate a Ring Handler for handling GraphQL requests. This is a more
   customisable version of [[handler]] since it's not bound to the alumbra
   parser by default.

   - `:parser`: a parser function for GraphQL documents (producing a value
     conforming to either `:alumbra/document` or `:alumbra/parser-errors`),
   - `:validator`: a function taking an `:alumbra/document` and producing either
     `nil` or a value conforming to `:alumbra/validation-errors`,
   - `:canonicalizer`: a function taking an `:alumbra/document` and producing
     an `:alumbra/canonical-document`,
   - `:context`: a function taking an HTTP request and returning a value
     representing the context of the GraphQL query,
   - `:executor`: an executor function, taking the request context, as well as
     a map conforming to `:alumbra/canonical-operation` and returning the resolved
     result.
   "
  [{:keys [parser validator canonicalizer executor context] :as opts}]
  {:pre [(fn? parser)
         (fn? validator)
         (fn? canonicalizer)
         (fn? executor)]}
  (-> (make-graphql-handler
        {:parser-fn       parser
         :validator-fn    validator
         :canonicalize-fn canonicalizer
         :executor-fn     executor
         :context-fn      context})
      (json/wrap-json-response)
      (json/wrap-json-body)))
