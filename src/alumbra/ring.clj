(ns alumbra.ring
  (:require [alumbra.ring.raw :refer [raw-handler]]
            [alumbra
             [analyzer :refer [analyze-schema]]
             [canonical :refer [canonicalize*]]
             [validator :refer [validator*]]
             [parser :as parser]]))


(defn  handler
  "Generate a Ring Handler for handling GraphQL requests, based on the alumbra
   parser, analyzer, validator and canonicaliser. (If you want to use different
   implementations you can use [[raw-handler]].)

   - `:schema`: a value representing a GraphQL schema (as string, file, input
   stream or as a map conforming to `:alumbra/schema`),
   - `:context`: a function taking an HTTP request and returning a value
   representing the context of the GraphQL query.
   - `:executor`: an executor function, taking the request context, as well as
   a map conforming to `:graphql/canonical-operation` and returning the resolved
   result.

   The resulting handler supports both Ring's classic style and the CPS one."
  [{:keys [schema context executor] :as opts}]
  (let [schema (analyze-schema schema)
        validator (validator* schema)]
    (->> {:parser        #(parser/parse-document %)
          :validator     validator
          :canonicalizer #(canonicalize* schema %1 %2)
          :context       context
          :executor      executor}
         (merge opts)
         (raw-handler))))

;; ## Test Code

(comment
  ((handler
     {:schema "type Person { id: ID! } schema { query: Person }"
      :executor prn})
   {:request-method :post
    :headers {"content-type" "application/json;charset=UTF-8"}
    :body    (java.io.ByteArrayInputStream.
               (.getBytes
                 (cheshire.core/generate-string
                   {:operationName nil
                    :query "{ id }"
                    :variables nil})
                 "UTF-8"))}))
