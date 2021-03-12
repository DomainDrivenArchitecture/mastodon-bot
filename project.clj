(defproject dda/mastodon-bot "1.10.9-SNAPSHOT"
  :description "Bot to publish twitter, tumblr or rss posts to an mastodon account."
  :url "https://github.com/yogthos/mastodon-bot"
  :author "Dmitri Sotnikov"
  :license {:name "MIT"}
  :dependencies []
  :source-paths ["src/main/cljc"
                 "src/main/clj"]
  :resource-paths ["src/main/resources"]
  :repositories [["releases" :clojars]
                 ["snapshots" :clojars]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]]
  :profiles {:test {:test-paths ["src/test/cljc"]
                    :resource-paths ["src/test/resources"]
                    :dependencies []}})
