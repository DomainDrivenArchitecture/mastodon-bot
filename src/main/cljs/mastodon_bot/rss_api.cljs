(ns mastodon-bot.rss-api
  (:require
   [orchestra.core :refer-macros [defn-spec]]
   [mastodon-bot.rss-domain :as rd]
   ["rss-parser" :as rss]))

(defn-spec rss-client any?
  []
  (rss.))

(defn-spec parse-feed any?
  [item ::rd/feed-item]  
  (let [{:keys [title isoDate pubDate content link]} item]
      {:created-at (js/Date. (or isoDate pubDate))
       :text (str title
                  "\n\n"
                  link)}))

(defn-spec get-feed map?
  [url string?
   callback fn?]
  (print url)
  (-> (.parseURL (rss-client) url)
      (.then callback)))
