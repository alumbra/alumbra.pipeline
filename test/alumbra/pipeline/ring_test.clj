(ns alumbra.pipeline.ring-test
  (:require [clojure.test :refer :all]
            [alumbra.pipeline
             [steps :as steps]
             [fixtures :as fix]
             [ring :as ring]]
            [claro.data :as data]
            [clojure.walk :as walk]))

;; ## Helpers

(defn- make-run
  [opts]
  (comp (juxt identity ring/as-response)
        (fix/partial-opts steps/run-ring-request opts)))

(defn- make-request
  [body]
  {:request-method :post
   :uri            "/graphql"
   :headers        {"content-type" "application/json;charset=UTF-8"}
   :body           (walk/stringify-keys body)})

(defrecord Inc [x]
  data/Resolvable
  (resolve! [_ _]
    (inc x)))

;; ## Tests

(deftest t-as-response
  (let [run (make-run {:query {:me {:name "Me"}
                               :inc (map->Inc {})}})]
    (testing "simple query."
      (let [[result {:keys [status body alumbra/pipeline-result]}]
            (run (make-request {:query "{ me { name } }"}))]
        (is (= pipeline-result result))
        (is (= 200 status))
        (is (= {:data (:data result)} body))))
    (testing "parser error."
      (let [[result {:keys [status body alumbra/pipeline-result]}]
            (run (make-request {:query "{ me { } }"}))]
        (is (= pipeline-result result))
        (is (= 400 status))
        (is (:errors body))))
    (testing "validation error."
      (let [[result {:keys [status body alumbra/pipeline-result]}]
            (run (make-request {:query "{ me { unknownField } }"}))]
        (is (= pipeline-result result))
        (is (= 400 status))
        (is (:errors body))))
    (testing "invalid request method."
      (let [[result {:keys [status body alumbra/pipeline-result]}]
            (run
              (-> {:query "{ me { unknownField } }"}
                  (make-request)
                  (assoc :request-method :put)))]
        (is (= pipeline-result result))
        (is (= 405 status))
        (is (:errors body))))
    (testing "invalid body"
      (let [[result {:keys [status body alumbra/pipeline-result]}]
            (run
              (-> {:queryString "{ me { unknownField } }"}
                  (make-request)))]
        (is (= pipeline-result result))
        (is (= 400 status))
        (is (:errors body)))
      (let [[result {:keys [status body alumbra/pipeline-result]}]
            (run (make-request nil))]
        (is (= pipeline-result result))
        (is (= 400 status))
        (is (:errors body))))
    (testing "variables given."
      (let [[result {:keys [status body alumbra/pipeline-result]}]
            (run (make-request
                   {:query "query ($x: Int!) { inc (x: $x) }"
                    :variables {"x" 9}}))]
        (is (= pipeline-result result))
        (is (= 200 status))
        (is (= {:data {"inc" 10}} body)))
      (let [[result {:keys [status body alumbra/pipeline-result]}]
            (run (make-request
                   {:query "query ($x: Int!) { inc (x: $x) }"
                    :variables "{\"x\": 9}"}))]
        (is (= pipeline-result result))
        (is (= 200 status))
        (is (= {:data {"inc" 10}} body))))
    (testing "variables missing."
      (let [[result {:keys [status body alumbra/pipeline-result]}]
            (run (make-request
                   {:query "query ($x: Int!) { inc (x: $x) }"
                    :variables {"y" 9}}))]
        (is (= pipeline-result result))
        (is (= 500 status))
        (is (:errors body))))))
