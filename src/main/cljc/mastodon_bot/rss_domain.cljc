(ns mastodon-bot.rss-domain
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::feed (s/cat :name string? :url string?))
(s/def ::feeds (s/coll-of ::feed))
(def rss-source?  (s/keys :req-un [::feeds]))

(s/def ::title string?)
(s/def ::content string?)
(s/def ::link string?)
(s/def ::author string?)
(s/def ::isoDate string?)
(s/def ::pubDate string?)
(s/def ::feed-item (s/keys :req-un [::title ::content ::link]
                           :opt-un [::author ::isoDate ::pubDate]))
