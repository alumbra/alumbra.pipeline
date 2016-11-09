(ns alumbra.ring
  (:require [ring.middleware.json :as json]
            [alumbra
             [analyzer :refer [analyze-schema]]
             [canonical :refer [canonicalize*]]
             [validator :refer [validator*]]
             [parser :as ql]]))

;; ## "Response Monad"

(deftype Response [response])
(alter-meta! #'->Response assoc :private true)

(defn- done!
  [response]
  (Response. response))

(defmacro ^:private r->>
  [value & forms]
  (if (seq forms)
    (let [[form & rst] forms]
      `(let [result# (->> ~value ~form)]
         (if (instance? Response result#)
           (.-response result#)
           (r->> result# ~@rst))))
    value))

;; ## Handler Fn

;; ### Errors

(defn- make-error-response
  [status error-message]
  {:status status
   :body   {:errors [{:message error-message}]}})

(defn- format-errors
  [status errors]
  ;; TODO
  {:status status
   :body   {:errors
            (for [{:keys [validator/error-class
                          validator/locations]} errors]
              (merge
                {:message (str "Error of class: " error-class)}
                (when (seq locations)
                  {:locations
                   (for [{:keys [validator/row
                                 validator/column]} locations]
                     {:row (inc row)
                      :column (inc column)})})))}})

;; ### Invalid Request Method

(def invalid-request-method-response
  (make-error-response
    405
    "Endpoint needs to be accesed using HTTP POST!"))

(defn- check-invalid-request-method
  [{:keys [request-method] :as request}]
  (if-not (= request-method :post)
    (done! invalid-request-method-response)
    request))

;; ### Invalid Body

(def invalid-body-response
  (make-error-response
    400
    (str "The HTTP body needs to be a JSON map of \"operationName\" (string), "
         "\"query\" (string) and \"variables\" (object)!")))

(defn- check-invalid-body
  [{:keys [body] :as request}]
  (if-not (and (some-> body map)
               (every? #(contains? body %)
                       ["operationName" "query" "variables"]))
    (done! invalid-body-response)
    request))

;; ### Prepare Pipeline

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

;; ### Parse GraphQL Query Document

(defn- parse-document
  [{:keys [parser-fn parse-error-fn]} {:keys [query] :as state}]
  (let [document (parser-fn query)]
    (if-let [errors (when parse-error-fn
                      (parse-error-fn document))]
      (done! {:status 400, :body errors})
      (assoc state :document document))))

;; ### Validate GraphQL Query Document

(defn- filter-operations
  [operation-name document]
  (if operation-name
    (update document
            :graphql/operations
            #(filter
               (comp #{operation-name}
                     :graphql/operation-name)
               %))
    document))

(defn- validate-document
  [{:keys [schema validator-fn]}
   {:keys [operation-name document variables] :as state}]
  (if-let [errors (validator-fn document variables)]
    (done! (format-errors 400 errors))
    (let [{:keys [graphql/operations]} document]
      (if (and (not operation-name) (next operations))
        (done! {:status 400})
        (let [document' (filter-operations operation-name document)]
          (if (-> document' :graphql/operations seq)
            (assoc state
                   :canonical-operation
                   (first (canonicalize* schema variables document')))
            (done! {:status 400})))))))

;; ### Execution

(defn- execute-document
  [{:keys [executor-fn]} {:keys [context canonical-operation]}]
  (executor-fn context canonical-operation))

;; ### Main Handler Function

(defn- handle-graphql-request
  [request opts]
  (r->> request
        (check-invalid-request-method)
        (check-invalid-body)
        (prepare-request opts)
        (parse-document opts)
        (validate-document opts)
        (execute-document opts)))

;; ## Handler Generation

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

(defn handler*
  "Generate a Ring Handler for handling GraphQL requests. This is a more
   customisable version of [[handler]] since it's not bound to the alumbra
   parser by default.

   - `:schema`: a GraphQL schema value,
   - `:parser`: a parser function for GraphQL documents (producing a map
   conforming to `:graphql/document`),
   - `:parse-error`: a function taking the output of `:parser` and returning
   either a GraphQL error map or `nil`,
   - `:context`: a function taking an HTTP request and returning a value
   representing the context of the GraphQL query.
   - `:executor`: an executor function, taking the request context, as well as
   a map conforming to `:graphql/canonical-operation` and returning the resolved
   result.

   "
  [{:keys [schema parser parse-error context executor]}]
  {:pre [schema
         (fn? parser)
         (fn? executor)]}
  (let [schema (analyze-schema schema)
        validator (validator* schema)]
    (-> (make-graphql-handler
          {:schema         schema
           :parser-fn      parser
           :parse-error-fn parse-error
           :validator-fn   validator
           :context-fn     context
           :executor-fn    executor})
        (json/wrap-json-response)
        (json/wrap-json-body))))

(defn  handler
  "Generate a Ring Handler for handling GraphQL requests.

   - `:schema`: a GraphQL schema value,
   - `:context`: a function taking an HTTP request and returning a value
   representing the context of the GraphQL query.
   - `:executor`: an executor function, taking the request context, as well as
   a map conforming to `:graphql/canonical-operation` and returning the resolved
   result.

   The resulting handler supports both Ring's classic style and the CPS one."
  [{:keys [schema context executor] :as opts}]
  (->> {:parser      ql/parse-document
        ;; FIXME
        :parse-error nil}
       (merge opts)
       (handler*)))

;; ## Test Code

(comment
  ((handler
     {:schema (ql/parse-schema
                "type Person { id: ID! } schema { query: Person }")
      :executor prn})
   {:request-method :post
    :headers {"content-type" "application/json;charset=UTF-8"}
    :body    (java.io.ByteArrayInputStream.
               (.getBytes
                 (cheshire.core/generate-string
                   {:operationName nil
                    :query "{ id } { id }"
                    :variables nil})
                 "UTF-8"))}))
