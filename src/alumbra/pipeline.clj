(ns alumbra.pipeline
  (:require [alumbra.pipeline
             [steps :as steps]
             [ring :as ring]]
            [ring.middleware.json :as json]))

;; ## Executor

(defn executor
  "Create a function that takes a query string, as well as an optional map of
   `:variables`, `:operation-name` and `:context` and produces the result of
   executing the query.

   ```clojure
   (def run-query
     (alumbra.pipeline/executor opts))

   (run-query \"{ me { name } }\")
   ;; => {:status :success, :data {\"me\" { ... }}, ...}
   ```

   See [[alumbra.pipeline.steps/run]] for the necessary `opts` and the expected
   result format."
  [opts]
  (fn [query & [{:keys [variables operation-name context]
                 :or   {variables {}}}]]
    (->> {:query          query
          :variables      variables
          :operation-name operation-name
          :context        context}
         (steps/run opts))))

;; ## Handler

(defn- make-graphql-handler
  [opts]
  (let [handler-fn (comp ring/as-response #(steps/run-ring-request opts %))]
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

   - `:parser-fn`: a parser function for GraphQL documents (producing a value
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
   and `\"variables\"`."
  [{:keys [parser-fn validator-fn canonicalize-fn executor-fn context-fn]}]
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
      (json/wrap-json-response)
      (json/wrap-json-body)))
