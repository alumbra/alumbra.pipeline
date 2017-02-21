(ns alumbra.ring.fixtures
  (:require [alumbra.ring.graphql :as graphql]
            [alumbra
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
       type QueryRoot { me: Person!, allPeople: [Person!] }
       schema { query: QueryRoot }"
      (analyzer/analyze-schema parser/parse-schema)))

;; ## Handler

(defn make-handler
  [executor-opts & [opts]]
  (graphql/handler
    (merge
      {:parser        parser/parse-document
       :validator     (validator/validator schema)
       :canonicalizer (analyzer/canonicalizer schema)
       :executor      (claro/executor (merge {:schema schema} executor-opts))}
      opts)))

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

(defn make-query
  [& args]
  (partial #'query (apply make-handler args)))
