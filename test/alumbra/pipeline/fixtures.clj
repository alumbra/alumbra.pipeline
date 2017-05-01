(ns alumbra.pipeline.fixtures
  (:require [alumbra
             [claro :as claro]
             [analyzer :as analyzer]
             [parser :as parser]
             [validator :as validator]]
            [cheshire.core :as json]))


;; ## Schema

(def schema
  (-> "type Person { name: String!, pets: [Pet!] }
       interface Pet { name: String! }
       type HouseCat implements Pet { name: String!, owner: Person!, meowVolume: Int }
       type HouseDog implements Pet { name: String!, owner: Person!, barkVolume: Int }
       type Cat implements Pet { name: String!, meowVolume: Int }
       type Dog implements Pet { name: String!, barkVolume: Int }
       type QueryRoot { me: Person!, allPeople: [Person!], inc(x:Int!): Int! }
       schema { query: QueryRoot }"
      (analyzer/analyze-schema parser/parse-schema)))

;; ## Handler

(defn make-opts
  [executor-opts & [opts]]
  (merge
    {:parser-fn       parser/parse-document
     :validator-fn    (validator/validator schema)
     :canonicalize-fn (analyzer/canonicalizer schema)
     :executor-fn     (claro/executor (merge {:schema schema} executor-opts))}
    opts))

(defn partial-opts
  [f opts]
  (partial f (make-opts opts)))

(defn- as-input-stream
  [body]
  (-> body
      (.getBytes "UTF-8")
      (java.io.ByteArrayInputStream.)))

(defn query
  [handler query & [body]]
  (let [request {:request-method :post
                 :headers {"content-type" "application/json;charset=UTF-8"}
                 :body (json/generate-string (merge {:query query} body))}]
    (-> request
        (update :body as-input-stream)
        (handler)
        (update :body #(some-> % (json/parse-string keyword)))
        (assoc :request request))))
