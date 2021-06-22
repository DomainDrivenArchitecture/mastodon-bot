(ns mastodon-bot.rss-api
  (:require
   [orchestra.core :refer-macros [defn-spec]]
   [mastodon-bot.rss-domain :as rd]
   [mastodon-bot.infra :as infra]
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
  [url string?]
  (infra/resolve-promise (.parseURL (rss-client) url) []))
