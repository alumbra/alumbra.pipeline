(defproject alumbra/ring "0.1.0-SNAPSHOT"
  :description "A Ring handler for GraphQL Execution"
  :url "https://github.com/alumbra/alumbra.ring"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2016
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [alumbra/validator "0.1.0-SNAPSHOT" :scope "provided"]
                 [alumbra/parser "0.1.0-SNAPSHOT" :scope "provided"]
                 [ring/ring-json "0.5.0-beta1"
                  :exclusions [commons-fileupload]]
                 [hiccup "1.0.5"]]
  :profiles {:dev {:dependencies [[alumbra/spec "0.1.0-SNAPSHOT"]
                                  [aleph "0.4.2-alpha8"]]}}
  :pedantic? :abort)
