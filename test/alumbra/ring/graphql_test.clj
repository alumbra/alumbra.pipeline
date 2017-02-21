(ns alumbra.ring.graphql-test
  (:require [clojure.test :refer :all]
            [alumbra.ring.fixtures :as fix]
            [claro.data :as data]
            [cheshire.core :as json]))

(deftest t-simple-query
  (let [query (fix/make-query
                {:query
                 {:me {:name "Me"}}})
        {:keys [status body]} (is (query "{ me { name } }"))]
    (is (= 200 status))
    (is (= {:data {:me {:name "Me"}}} body))))

(deftest t-bad-query
  (let [query (fix/make-query
                {:query
                 {:me {:name "Me"}}})
        {:keys [status body]} (is (query "{ me { } }"))]
    (is (= 400 status))
    (let [{:keys [locations context]} (-> body :errors first)]
      (is (= [{:row 1, :column 8}] locations))
      (is (= {:type "parser-error"} context)))))

(deftest t-invalid-query
  (let [query (fix/make-query
                {:query
                 {:me {:name "Me"}}})
        {:keys [status body]} (is (query "{ me { unknownField } }"))]
    (is (= 400 status))
    (let [{:keys [locations context]} (-> body :errors first)]
      (is (= [{:row 1, :column 8}] locations))
      (is (= {:type "validation-error"} context)))))

(deftest t-partially-failing-query
  (let [query (fix/make-query
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
  (let [query (fix/make-query
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
