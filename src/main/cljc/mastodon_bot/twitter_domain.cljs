(ns mastodon-bot.twitter-domain
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::consumer_key string?)
(s/def ::consumer_secret string?)
(s/def ::access_token_key string?)
(s/def ::access_token_secret string?)
(def twitter-auth? (s/keys :req-un [::consumer_key ::consumer_secret ::access_token_key 
                                      ::access_token_secret]))

(s/def ::include-rts? boolean?)
(s/def ::include-replies? boolean?)
(s/def ::nitter-urls? boolean?)
(s/def ::account string?)
(s/def ::accounts (s/* ::account))
(def twitter-source?  (s/keys :req-un [::include-rts? ::include-replies? ::accounts]
                              :opt-un [::nitter-urls?]))
