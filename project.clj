(defproject dynamo/server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://danielytics.github.io/dynamo-server/"
  :license {:name "MIT License"
            :url "https://github.com/danielytics/dynamo-server/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.7.0-beta3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.stuartsierra/component "0.2.3"]
                 [juxt.modular/aleph "0.0.8" :exclusions [aleph]]
                 [aleph "0.4.0"]
                 [gloss "0.2.5"]
                 [instaparse "1.4.0"]
                 [org.clojars.pjlegato/clansi "1.3.0"]
                 [reloaded.repl "0.1.0"]])
