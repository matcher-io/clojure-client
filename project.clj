(defproject matcher-client "1.0"
  :min-lein-version "2.0.0"
  :description "matcher.io client library for Clojure"
  :url "http://www.matcher.io"
  :dependencies [
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.2.0"]
                 [com.novemberain/langohr "1.0.0-beta9"]
                 [org.clojure/tools.logging "0.2.3"]
                 [log4j/log4j "1.2.17"]
                ] 
  :test-paths ["src.test"]
)
