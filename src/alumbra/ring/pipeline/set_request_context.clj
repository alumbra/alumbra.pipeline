(ns alumbra.ring.pipeline.set-request-context)

(defn set-request-context
  [{:keys [context-fn]} {:keys [request] :as state}]
  (assoc state
         :context
         (if context-fn
           (context-fn request)
           {})))
