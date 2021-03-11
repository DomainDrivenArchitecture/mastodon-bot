(ns mastodon-bot.transform-domain
  (:require
   [clojure.spec.alpha :as s]
   [mastodon-bot.mastodon-domain :as md]
   [mastodon-bot.twitter-domain :as twd]
   [mastodon-bot.rss-domain :as rd]
   [mastodon-bot.tumblr-domain :as td]))

(s/def ::created-at any?)
(s/def ::text string?)
(s/def ::untrimmed-text string?)
(s/def ::media-links string?)
(s/def ::screen_name string?)
(def intermediate?  (s/keys :req-un [::created-at ::text ::screen_name]
                     :opt-un [::media-links ::untrimmed-text]))

(s/def ::source-type #{:twitter :rss :tumblr})
(s/def ::resolve-urls? boolean?)
(s/def ::content-filter string?)
(s/def ::content-filters (s/* ::content-filter))
(s/def ::keyword-filter string?)
(s/def ::keyword-filters (s/* ::keyword-filter))
(s/def ::replacements any?)
(defmulti source-type :source-type)
(defmethod source-type :twitter [_]
  (s/merge (s/keys :req-un[::source-type]) twd/twitter-source?))
(defmethod source-type :rss [_]
  (s/merge (s/keys :req-un [::source-type]) rd/rss-source?))
(defmethod source-type :tumblr [_]
  (s/merge (s/keys :req-un [::source-type]) td/tumblr-source?))
(s/def ::source (s/multi-spec source-type ::source-type))

(s/def ::target-type #{:mastodon})
(defmulti target-type :target-type)
(defmethod target-type :mastodon [_]
  (s/merge (s/keys :req-un [::target-type]) md/mastodon-target?))
(s/def ::target (s/multi-spec target-type ::target-type))

(s/def ::transformation (s/keys :req-un [::source ::target]
                                :opt-un [::resolve-urls? ::content-filters ::keyword-filters 
                                         ::replacements]))
(def transformations? (s/* ::transformation))
