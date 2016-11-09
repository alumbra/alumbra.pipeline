(defproject alumbra/ring "0.1.0-SNAPSHOT"
  :description "A Ring handler for GraphQL Execution"
  :url "https://github.com/alumbra/alumbra.ring"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2016
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [alumbra/spec "0.1.0-SNAPSHOT"]
                 [alumbra/validator "0.1.0-SNAPSHOT"]
                 [ring/ring-json "0.5.0-beta1"
                  :exclusions [commons-fileupload]]]
  :profiles {:dev {:dependencies [[aleph "0.4.1"]]}}
  :pedantic? :abort)
