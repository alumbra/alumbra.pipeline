(ns alumbra.pipeline.steps.core)

;; ## Possible Results

(defprotocol Result
  (value [this])
  (error [this]))

(extend-protocol Result
  Object
  (value [this]
    this)
  (error [_]
    nil)

  nil
  (value [this]
    this)
  (error [_]
    nil))

(deftype Failure [value error]
  Result
  (value [_]
    value)
  (error [_]
    error))
(alter-meta! #'->Failure assoc :private true)

(defn failure!
  [value error]
  (Failure. value error))

;; ## Predicates/Accessors

(defmacro pipeline->>
  [value & forms]
  (if (seq forms)
    (let [[form & rst] forms]
      `(let [value# ~value]
         (if-not (instance? Failure value#)
           (pipeline->>
             (try
               (->> value# ~form)
               (catch Throwable t#
                 (failure! value# t#)))
             ~@rst)
           value#)))
    value))
