(ns alumbra.ring.pipeline.core)

(deftype Done [value])
(alter-meta! #'->Done assoc :private true)

(defn done!
  [value]
  (Done. value))

(defmacro pipeline->>
  [value & forms]
  (if (seq forms)
    (let [[form & rst] forms]
      `(let [result# (->> ~value ~form)]
         (if (instance? Done result#)
           (.-value result#)
           (pipeline->> result# ~@rst))))
    value))
