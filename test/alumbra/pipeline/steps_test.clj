(ns alumbra.pipeline.steps-test
  (:require [clojure.test :refer :all]
            [alumbra.pipeline.steps :as steps]
            [alumbra.pipeline.fixtures :as fix]
            [claro.data :as data]
            [clojure.walk :as walk]))

;; ## Helpers

(defn- error-locations
  [{:keys [alumbra/parser-errors
           alumbra/validation-errors]}]
  (if parser-errors
    (map :alumbra/location parser-errors)
    (mapcat :alumbra/locations validation-errors)))

(defrecord Inc [x]
  data/Resolvable
  (resolve! [_ _]
    (inc x)))

;; ## Raw Run

(defn- make-run
  [opts]
  (fix/partial-opts steps/run opts))

(deftest t-run
  (let [run (make-run {:query {:me {:name "Me"}}})]
    (testing "simple query."
      (let [{:keys [status data errors]}
            (run {:query "{ me { name } }"})]
        (is (= :success status))
        (is (= {"me" {"name" "Me"}} data))
        (is (nil? errors))))
    (testing "bad query."
      (let [{:keys [status data error errors]}
            (run {:query "{ me { } }"})]
        (is (= :error status))
        (is (= :parser-error error))
        (is (nil? data))
        (is (= [{:row 0, :column 7, :index 7}] (error-locations errors)))))
    (testing "invalid query."
      (let [{:keys [status data error errors]}
            (run {:query "{ me { unknownField } }"})]
        (is (= :error status))
        (is (= :validation-error error))
        (is (nil? data))
        (is (= [{:row 0, :column 7, :index 7}] (error-locations errors)))))))

(deftest t-run-with-variables
  (let [runner (make-run {:query {:inc (map->Inc {})}})
        run #(runner {:query %1, :variables %2})]
    (testing "variables given."
      (let [{:keys [status data]}
            (run
              "query ($x: Int!) { inc(x: $x) }"
              {"x" 9})]
        (is (= :success status))
        (is (= {"inc" 10} data))))
    (testing "variables missing."
      (let [{:keys [status exception data]}
            (run
              "query ($x: Int!) { inc(x: $x) }"
              {})]
        (is (= :exception status))
        (is (= "variable missing: $x"
               (.getMessage exception)))
        (is (nil? data))))))

(deftest t-run-with-partial-result
  (let [run
        (make-run
          {:query
           {:me (reify data/Resolvable
                  (resolve! [_ _]
                    (data/error "some error." {:some "context"})))}})
        {:keys [status error errors]}
        (run {:query "{ me { name } }"})]
    (is (= :error status))
    (is (= :execution-error error))
    (is (= [{:message "some error.", :context {:some "context"}}]
           (:alumbra/execution-errors errors)))))

(deftest t-run-with-exception
  (let [run
        (make-run
          {:query
           {:me (reify data/Resolvable
                  (resolve! [_ _]
                    (throw (Exception."some error."))))}})
        {:keys [status exception]}
        (run {:query "{ me { name } }"})]
    (is (= :exception status))
    (is (= "some error." (.getMessage exception)))))

;; ## Request Runner

(defn- make-ring-run
  [opts]
  (fix/partial-opts steps/run-ring-request opts))

(defn- make-request
  [body]
  {:request-method :post
   :uri            "/graphql"
   :headers        {"content-type" "application/json;charset=UTF-8"}
   :body           (walk/stringify-keys body)})

(deftest t-run-ring-request
  (let [run (make-ring-run {:query {:me {:name "Me"}}})]
    (testing "simple query."
      (let [{:keys [status error data errors]}
            (run (make-request {:query "{ me { name } }"})) ]
        (is (= :success status))
        (is (= {"me" {"name" "Me"}} data))
        (is (nil? error))
        (is (nil? errors))))
    (testing "bad query."
      (let [{:keys [status data error errors]}
            (run (make-request {:query "{ me { } }"})) ]
        (is (= :error status))
        (is (= :parser-error error))
        (is (nil? data))
        (is (= [{:row 0, :column 7, :index 7}] (error-locations errors)))))
    (testing "invalid query."
      (let [{:keys [status data error errors]}
            (run (make-request {:query "{ me { unknownField } }"}))]
        (is (= :error status))
        (is (= :validation-error error))
        (is (nil? data))
        (is (= [{:row 0, :column 7, :index 7}] (error-locations errors)))))
    (testing "invalid request method."
      (let [{:keys [status data error errors]}
            (run
              (-> (make-request {:query "{ me { unknownField } }"})
                  (assoc :request-method :put)))]
        (is (= :error status))
        (is (= :invalid-request-method error))
        (is (nil? data))))
    (testing "invalid body."
      (let [{:keys [status data error errors]}
            (run (make-request {:queryString "{ me { unknownField } }"}))]
        (is (= :error status))
        (is (= :invalid-request-body error))
        (is (nil? data)))
      (let [{:keys [status data error errors]} (run (make-request nil))]
        (is (= :error status))
        (is (= :invalid-request-body error))
        (is (nil? data))))))

(deftest t-run-ring-request-with-variables
  (let [runner (make-ring-run {:query {:inc (map->Inc {})}})
        run #(runner (make-request {:query %1, :variables %2}))]
    (testing "variables given."
      (let [{:keys [status error data]}
            (run
              "query ($x: Int!) { inc(x: $x) }"
              {"x" 9})]
        (is (= :success status))
        (is (nil? error))
        (is (= {"inc" 10} data))))
    (testing "variables as JSON string (backwards-compatibility)."
      (let [{:keys [status error data]}
            (run
              "query ($x: Int!) { inc(x: $x) }"
              "{\"x\": 9}")]
        (is (= :success status))
        (is (nil? error))
        (is (= {"inc" 10} data)))
      (let [{:keys [status error data]}
            (run
              "query ($x: Int!) { inc(x: $x) }"
              "{\"x: 9}")]
        (is (= :error status))
        (is (= :invalid-variables-json error))
        (is (nil? data))))
    (testing "variables missing."
      (let [{:keys [status exception data]}
            (run
              "query ($x: Int!) { inc(x: $x) }"
              {})]
        (is (= :exception status))
        (is (= "variable missing: $x"
               (.getMessage exception)))
        (is (nil? data))))))

(deftest t-run-ring-request-with-partial-result
  (let [run
        (make-ring-run
          {:query
           {:me (reify data/Resolvable
                  (resolve! [_ _]
                    (data/error "some error." {:some "context"})))}})
        {:keys [status error errors]}
        (run (make-request {:query "{ me { name } }"}))]
    (is (= :error status))
    (is (= :execution-error error))
    (is (= [{:message "some error.", :context {:some "context"}}]
           (:alumbra/execution-errors errors)))))

(deftest t-run-ring-request-with-exception
  (let [run
        (make-ring-run
          {:query
           {:me (reify data/Resolvable
                  (resolve! [_ _]
                    (throw (Exception."some error."))))}})
        {:keys [status exception]}
        (run (make-request {:query "{ me { name } }"}))]
    (is (= :exception status))
    (is (= "some error." (.getMessage exception)))))
