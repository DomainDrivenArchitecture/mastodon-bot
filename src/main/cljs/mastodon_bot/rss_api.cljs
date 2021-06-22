(ns mastodon-bot.rss-api
  (:require
   [orchestra.core :refer-macros [defn-spec]]
   [mastodon-bot.rss-domain :as rd]
   [clojure.spec.alpha :as s]
   ["rss-parser" :as rss]))

(s/def ::pos-integer (and #(< 0 %) integer?))
(defn-spec rss-client any?
  [& {:keys [timeout]
      :or {timeout 3000}} (s/keys :opt-un [::pos-integer])]
  (rss. #js {:timeout timeout}))

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
      (.then callback)
      (.catch #(js/console.log %))))
