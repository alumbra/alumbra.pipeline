(ns alumbra.pipeline-test
  (:require [clojure.test :refer :all]
            [alumbra.pipeline.fixtures :as fix]
            [alumbra.pipeline :as pipeline]
            [claro.data :as data]
            [cheshire.core :as json]))

(defn make-query
  [opts]
  (partial fix/query
           (pipeline/handler (fix/make-opts opts))))

(deftest t-simple-query
  (let [query (make-query
                {:query
                 {:me {:name "Me"}}})
        {:keys [status body]} (is (query "{ me { name } }"))]
    (is (= 200 status))
    (is (= {:data {:me {:name "Me"}}} body))))

(deftest t-bad-query
  (let [query (make-query
                {:query
                 {:me {:name "Me"}}})
        {:keys [status body]} (is (query "{ me { } }"))]
    (is (= 400 status))
    (let [{:keys [message locations context]} (-> body :errors first)]
      (is (re-find #"mismatched input '}'" message))
      (is (= [{:row 1, :column 8}] locations))
      (is (= "parser-error" (:type context))))))

(deftest t-invalid-query
  (let [query (make-query
                {:query
                 {:me {:name "Me"}}})
        {:keys [status body]} (is (query "{ me { unknownField } }"))]
    (is (= 400 status))
    (let [{:keys [locations context]} (-> body :errors first)]
      (is (= [{:row 1, :column 8}] locations))
      (is (= "validation-error" (:type context))))))

(deftest t-partially-failing-query
  (let [query (make-query
                {:query
                 {:me (reify data/Resolvable
                        (resolve! [_ _]
                          (data/error "some error." {:some "context"})))}})
        {:keys [status body]} (is (query "{ me { name } }"))]
    (is (= 500 status))
    (is (= {:data {:me nil}
            :errors [{:message "some error."
                      :context {:some "context"}}]}
           body))))

(deftest t-exception-query
  (let [query (make-query
                {:query
                 {:me (reify data/Resolvable
                        (resolve! [_ _]
                          (throw (Exception. "some error."))))}})
        {:keys [status body]} (is (query "{ me { name } }"))]
    (is (= 500 status))
    (is (= {:errors [{:message "[Exception] some error."
                      :context {:type "uncaught-exception"
                                :exception-class "java.lang.Exception"}}]}
           body))))
