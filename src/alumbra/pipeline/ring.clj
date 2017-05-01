(ns alumbra.pipeline.ring
  (:require [alumbra.errors :as errors]
            [clojure.set :refer [rename-keys]]))

;; ## Success

(defn- success-response
  [{:keys [data]}]
  {:status 200
   :body   {:data data}})

;; ## Exception

(defn- exception-response
  [{^Throwable t :exception}]
  (let [exception-class (class t)
        message (format "[%s] %s"
                        (.getSimpleName exception-class)
                        (or (.getMessage t) "<no message>"))]
    {:status 500
     :body
     {:errors
      [{:message message
        :context {:type            "uncaught-exception"
                  :exception-class (.getName exception-class)}}]}
     :alumbra/exception t}))

;; ## Errors

(defn- single-error-response
  [status error-message]
  {:status status
   :body   {:errors [{:message error-message}]}})

(defn- format-error
  [{:keys [context hint] :as error} error-type]
  (-> error
      (update :locations
              #(mapv
                 (fn [location]
                   (-> location
                       (update :row inc)
                       (update :column inc)
                       (dissoc :index)))
                 %))
      (merge
        {:context
         (cond-> {:type  error-type}
           context (assoc :query context)
           hint    (assoc :hint  hint))})
      (select-keys [:locations :message :context])))

(defn- validation-error-response
  [{:keys [errors query]}]
  {:status 400
   :body {:errors (->> (errors/explain-data errors query)
                       (mapv #(format-error % "validation-error")))}})

(defn- parser-error-response
  [{:keys [errors query]}]
  {:status 400
   :body {:errors (->> (errors/explain-data errors query)
                       (mapv #(format-error % "parser-error")))}})

(defn- execution-error-response
  [{:keys [data errors]}]
  {:status 500
   :body   {:data   data
            :errors (:alumbra/execution-errors errors)}})

(defn- invalid-request-method-response
  [_]
  (single-error-response
    405
    "Endpoint needs to be accessed using HTTP POST!"))

(defn- invalid-request-body-response
  [_]
  (single-error-response
    400
    "The HTTP body needs to be a JSON map with at least the 'query' field!"))

(defn- invalid-variables-response
  [_]
  (single-error-response
    400
    "The 'variables' field needs to be given as a JSON object."))

(defn- invalid-variables-json-response
  [_]
  (single-error-response
    400
    (str "If the 'variables' field is given as a string, "
         "it needs to contain valid JSON")))

(defn- error-response
  [{:keys [error] :as pipeline-result}]
  (case error
    :validation-error       (validation-error-response pipeline-result)
    :parser-error           (parser-error-response pipeline-result)
    :execution-error        (execution-error-response pipeline-result)
    :invalid-request-method (invalid-request-method-response pipeline-result)
    :invalid-request-body   (invalid-request-body-response pipeline-result)
    :invalid-variables      (invalid-variables-response pipeline-result)
    :invalid-variables-json (invalid-variables-json-response pipeline-result)))

;; ## Formatter

(defn as-response
  "Take the result of [[alumbra.pipeline.steps/run]] and generate a Ring
   response describing it."
  [{:keys [status] :as pipeline-result}]
  (merge
    (case status
      :success   (success-response pipeline-result)
      :exception (exception-response pipeline-result)
      :error     (error-response pipeline-result))
    {:alumbra/pipeline-result pipeline-result}))
