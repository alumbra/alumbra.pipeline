(ns alumbra.ring.pipeline-test
  (:require [clojure.test :refer :all]
            [alumbra.ring.pipeline :as pipeline]
            [alumbra.ring.fixtures :as fix]
            [claro.data :as data]))

(defn- make-runner
  [opts]
  (let [opts (fix/make-opts opts)]
    (partial pipeline/run-query opts)))

(let [run (make-runner {:query {:me {:name "Me"}}})]
  (deftest t-simple-query
    (is (= {:data {:me {:name "Me"}}}
           (run "{ me { name } }"))))

  (deftest t-bad-query
    (let [result (is (run "{ me { } }"))
          {:keys [locations context]} (-> result :errors first)]
      (is (= [{:row 1, :column 8}] locations))
      (is (= {:type "parser-error"} context))))

  (deftest t-invalid-query
    (let [result (is (run "{ me { unknownField } }"))
          {:keys [locations context]} (-> result :errors first)]
      (is (= [{:row 1, :column 8}] locations))
      (is (= {:type "validation-error"} context)))))

(defrecord Inc [x]
  data/Resolvable
  (resolve! [_ _]
    (inc x)))

(deftest t-query-with-variables
  (let [run (make-runner
              {:query {:inc (map->Inc {})}})]
    (is (= {:data {:inc 10}}
           (run
             "query ($x: Int!) { inc (x: $x) }"
             {:variables {"x" 9}})))))

(deftest t-partially-failing-query
  (let [run (make-runner
              {:query
               {:me (reify data/Resolvable
                      (resolve! [_ _]
                        (data/error "some error." {:some "context"})))}})
        result (is (run "{ me { name } }"))]
    (is (= {:data {:me nil}
            :errors [{:message "some error."
                      :context {:some "context"}}]}
           result))))

(deftest t-exception-query
  (let [run (make-runner
              {:query
               {:me (reify data/Resolvable
                      (resolve! [_ _]
                        (throw (Exception. "some error."))))}})]
    (is
      (thrown-with-msg?
        Exception
        #"some error\."
        (run "{ me { name } }")))))
