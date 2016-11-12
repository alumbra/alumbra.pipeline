(ns alumbra.ring.pipeline.canonicalize-operation
  (:require [alumbra.ring.pipeline.core :refer [done!]]))

(defn canonicalize-operation
  [{:keys [canonicalize-fn]}
   {:keys [document operation-name variables] :as state}]
  (assoc state
         :canonical-operation
         (canonicalize-fn document variables operation-name)))
