(defproject dda/mastodon-bot "1.12.1-SNAPSHOT"
  :description "Bot to publish twitter, tumblr or rss posts to an mastodon account."
  :url "https://github.com/yogthos/mastodon-bot"
  :author "Dmitri Sotnikov"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.10.3"]]
  :source-paths ["src/main/cljc"
                 "src/main/clj"]
  :resource-paths ["src/main/resources"]
  :repositories [["releases" :clojars]
                 ["snapshots" :clojars]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]]
  :profiles {:test {:test-paths ["src/test/cljc"]
                    :resource-paths ["src/test/resources"]
                    :dependencies []}}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]])
