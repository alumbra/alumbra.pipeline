(ns alumbra.ring.errors)

;; ## Single Error

(defn single-error-response
  [status error-message]
  {:status status
   :body   {:errors [{:message error-message}]}})

(defn error-response
  [status errors]
  {:status status
   :body {:errors errors}})

(defn exception-response
  [^Throwable t]
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

;; ## Middleware

(defn wrap
  [handler]
  (fn
    ([request]
     (try
       (handler request)
       (catch Throwable t
         (exception-response t))))
    ([request respond raise]
     (try
       (handler request respond raise)
       (catch Throwable t
         (respond (exception-response t)))))))

;; ## Error Formatting

(defn format-validation-errors
  [validation-errors]
  (for [{:keys [alumbra/validation-error-class
                alumbra/locations]
         :as error}
        validation-errors]
    (merge
      {:message (str "Error of class: " validation-error-class)
       :context {:type "validation-error"}
       :alumbra/validation-error error}
      (when (seq locations)
        {:locations
         (for [{:keys [row column]} locations]
           {:row (inc row)
            :column (inc column)})}))))

(defn validation-error-response
  [validation-errors]
  (->> (format-validation-errors validation-errors)
       (error-response 400)))

(defn format-parser-errors
  [parser-errors]
  (for [{:keys [alumbra/parser-error-message
                alumbra/location]} parser-errors
        :let [{:keys [row column]} location]]
    {:message
     (format "Syntax Error GraphQL Request (%d:%d) %s"
             (inc row)
             (inc column)
             parser-error-message)
     :locations
     [{:row (inc row)
       :column (inc column)}]
     :context {:type "parser-error"}}))

(defn parser-error-response
  [parser-errors]
  (->> (format-parser-errors parser-errors)
       (error-response 400)))
