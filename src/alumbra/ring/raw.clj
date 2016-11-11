(ns alumbra.ring.raw
  (:require [ring.middleware.json :as json]))

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
            (for [{:keys [alumbra/validation-error-class
                          alumbra/locations]} errors]
              (merge
                {:message (str "Error of class: " validation-error-class)}
                (when (seq locations)
                  {:locations
                   (for [{:keys [row column]} locations]
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
  [{:keys [parser-fn]} {:keys [query] :as state}]
  (let [document (parser-fn query)]
    (if (map? document)
      (assoc state :document document)
      (done! {:status 400, :body document}))))

;; ### Validate GraphQL Query Document

(defn- validate-document
  [{:keys [validator-fn]}
   {:keys [document variables] :as state}]
  (if-let [errors (validator-fn document variables)]
    (done! (format-errors 400 errors))
    state))

;; ### Canonicalize

(defn- filter-operations
  [operation-name document]
  (if operation-name
    (update document
            :alumbra/operations
            #(filter
               (comp #{operation-name}
                     :alumbra/operation-name)
               %))
    document))

(defn- canonicalize-operation
  [{:keys [canonicalize-fn]}
   {:keys [operation-name document variables] :as state}]
  (let [{:keys [alumbra/operations]} document]
    (if (and (not operation-name) (next operations))
      (done! {:status 400})
      (let [document' (filter-operations operation-name document)]
        (if (-> document' :alumbra/operations seq)
          (assoc state
                 :canonical-operation
                 (first (canonicalize-fn variables document')))
          (done! {:status 400}))))))

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
        (canonicalize-operation opts)
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
