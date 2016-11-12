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

;; ## Error Formatting

(defn format-validation-errors
  [validation-errors]
  (for [{:keys [alumbra/validation-error-class
                alumbra/locations]}
        validation-errors]
    (merge
      {:message (str "Error of class: " validation-error-class)}
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
       :column (inc column)}]}))

(defn parser-error-response
  [parser-errors]
  (->> (format-parser-errors parser-errors)
       (error-response 400)))
