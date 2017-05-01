(ns alumbra.pipeline.steps.canonicalize-operation)

(defn canonicalize-operation
  [{:keys [canonicalize-fn]}
   {:keys [document operation-name variables] :as state}]
  (assoc state
         :canonical-operation
         (canonicalize-fn document operation-name variables)))
