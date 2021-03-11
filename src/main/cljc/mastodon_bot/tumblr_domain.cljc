(ns mastodon-bot.tumblr-domain
  (:require
   [clojure.spec.alpha :as s]
   ))

(s/def ::consumer_key string?)
(s/def ::consumer_secret string?)
(s/def ::token string?)
(s/def ::token_secret string?)
(def tumblr-auth? (s/keys :req-un [::consumer_key ::consumer_secret ::token
                                    ::token_secret]))

(s/def ::limit pos?)
(s/def ::account string?)
(s/def ::accounts (s/* ::account))
(def tumblr-source?  (s/keys :req-un [::limit ::accounts]))
