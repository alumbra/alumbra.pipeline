(defproject alumbra/pipeline "0.2.0"
  :description "A pipeline for GraphQL execution, as well as a Ring handler."
  :url "https://github.com/alumbra/alumbra.ring"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2016
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [alumbra/spec "0.1.6" :scope "provided"]
                 [alumbra/errors "0.1.0"]
                 [ring/ring-json "0.5.0-beta1"
                  :exclusions [commons-fileupload]]
                 [hiccup "1.0.5"]]
  :profiles {:dev
             {:dependencies
              [[alumbra/validator "0.2.1"]
               [alumbra/parser "0.1.6"]
               [alumbra/analyzer "0.1.12" :exclusions [riddley]]
               [alumbra/claro "0.1.10"]
               [aleph "0.4.3"]]}
             :codox
             {:plugins [[lein-codox "0.10.3"]]
              :dependencies [[codox-theme-rdash "0.1.2"]]
              :codox {:project {:name "alumbra.ring"}
                      :metadata {:doc/format :markdown}
                      :themes [:rdash]
                      :source-uri "https://github.com/alumbra/alumbra.pipeline/blob/v{version}/{filepath}#L{line}"
                      :namespaces [alumbra.pipeline
                                   alumbra.pipeline.ring
                                   alumbra.pipeline.steps]}}}
  :aliases {"codox" ["with-profile" "+codox" "codox"]}
  :pedantic? :abort)
