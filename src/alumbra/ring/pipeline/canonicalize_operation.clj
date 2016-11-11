(ns alumbra.ring.pipeline.canonicalize-operation
  (:require [alumbra.ring.pipeline.core :refer [done!]]))

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

(defn canonicalize-operation
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
