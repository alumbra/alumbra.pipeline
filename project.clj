(defproject alumbra/ring "0.1.4-SNAPSHOT"
  :description "A Ring handler for GraphQL Execution"
  :url "https://github.com/alumbra/alumbra.ring"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2016
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [alumbra/spec "0.1.6" :scope "provided"]
                 [ring/ring-json "0.5.0-beta1"
                  :exclusions [commons-fileupload]]
                 [hiccup "1.0.5"]]
  :profiles {:dev
             {:dependencies
              [[alumbra/validator "0.2.0"]
               [alumbra/parser "0.1.6"]
               [alumbra/analyzer "0.1.10" :exclusions [riddley]]
               [alumbra/claro "0.1.8"]
               [aleph "0.4.3"]]}
             :codox
             {:plugins [[lein-codox "0.10.3"]]
              :dependencies [[codox-theme-rdash "0.1.2"]]
              :codox {:project {:name "alumbra.ring"}
                      :metadata {:doc/format :markdown}
                      :themes [:rdash]
                      :source-uri "https://github.com/alumbra/alumbra.ring/blob/v{version}/{filepath}#L{line}"
                      :namespaces [alumbra.ring.graphql
                                   alumbra.ring.graphiql
                                   alumbra.ring.pipeline
                                   alumbra.ring.errors]}}}
  :aliases {"codox" ["with-profile" "+codox" "codox"]}
  :pedantic? :abort)
