(ns alumbra.ring.graphiql
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [clojure.java.io :as io]
            [clojure.string :as string]))

;; ## License for GraphiQL
;;
;; LICENSE AGREEMENT For GraphiQL software
;;
;; Facebook, Inc. (“Facebook”) owns all right, title and interest, including all
;; intellectual property and other proprietary rights, in and to the GraphiQL
;; software. Subject to your compliance with these terms, you are hereby granted a
;; non-exclusive, worldwide, royalty-free copyright license to (1) use and copy the
;; GraphiQL software; and (2) reproduce and distribute the GraphiQL software as
;; part of your own software (“Your Software”). Facebook reserves all rights not
;; expressly granted to you in this license agreement.
;;
;; THE SOFTWARE AND DOCUMENTATION, IF ANY, ARE PROVIDED "AS IS" AND ANY EXPRESS OR
;; IMPLIED WARRANTIES (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
;; MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE DISCLAIMED. IN NO
;; EVENT SHALL FACEBOOK OR ITS AFFILIATES, OFFICES, DIRECTORS OR EMPLOYEES BE
;; LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
;; CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
;; GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
;; HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
;; LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
;; THE USE OF THE SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
;;
;; You will include in Your Software (e.g., in the file(s), documentation or other
;; materials accompanying your software): (1) the disclaimer set forth above; (2)
;; this sentence; and (3) the following copyright notice:
;;
;; Copyright (c) 2015, Facebook, Inc. All rights reserved.

;; ## CDN Helper

(defn- cdn-path
  ^String
  [group-name filename version]
  (str "//cdnjs.cloudflare.com/ajax/libs/"
       group-name
       "/"
       version
       "/"
       filename))

(defn- cdn
  [group-name filename version]
  (let [path (cdn-path group-name filename version)]
    (if (.endsWith path ".js")
      (include-js path)
      (include-css path))))

;; ## Rendering

(defn- render-graphiql
  [path container-id]
  [:script
   (-> (io/resource "alumbra/graphiql-init.js")
       (slurp)
       (string/replace "%%path%%" path)
       (string/replace "%%container%%" (name container-id)))])

;; ## Handler

(defn handler
  "Generate a Ring handler that will render the GraphiQL UI, pointing at the
   given URL/path. Assets will be included using a CDN unless `use-cdn?` is
   set to `false` (in which case you'll want to inject the assets directly
   into the generated Hiccup, e.g. using `custom-head-tags`)."
  [graphql-path
   & [{:keys [graphiql-version
              promise-version
              fetch-version
              react-version
              use-cdn?
              title
              custom-head-tags]
       :or {graphiql-version "0.8.0"
            promise-version  "4.0.5"
            fetch-version    "0.9.0"
            react-version    "15.3.2"
            use-cdn?         true
            title            "GraphiQL"}}]]
  (let [head-tag
        [:head
         [:style
          "body { height: 100%; width: 100%; margin: 0; overflow: hidden; }"
          "#graphiql { height: 100vh; }"]
         [:title title]
         (when use-cdn?
           (list
             (cdn "es6-promise" "es6-promise.auto.min.js" promise-version)
             (cdn "fetch"       "fetch.min.js"            fetch-version)
             (cdn "react"       "react.min.js"            react-version)
             (cdn "react"       "react-dom.min.js"        react-version)
             (cdn "graphiql"    "graphiql.min.js"         graphiql-version)
             (cdn "graphiql"    "graphiql.min.css"        graphiql-version)))
         custom-head-tags]
        body-tag
        [:body
         [:div#graphiql "Loading GraphiQL ..."]
         (render-graphiql graphql-path "graphiql")]
        response {:status  200
                  :headers {"content-type" "text/html;charset=UTF-8"}
                  :body    (html5 head-tag body-tag)}]
    (fn
      ([_] response)
      ([_ respond _] (respond response)))))
