(ns alumbra.ring.pipeline.validate-document
  (:require [alumbra.ring.pipeline.core :refer [done!]]
            [alumbra.ring.errors :refer [validation-error-response]]))

(defn validate-document
  [{:keys [validator-fn]}
   {:keys [document variables] :as state}]
  (if-let [errors (validator-fn document variables)]
    (done! (validation-error-response 400 errors))
    state))
