(ns mastodon-bot.core-domain
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as cs]
   [mastodon-bot.transform-domain :as trd]
   [mastodon-bot.mastodon-domain :as md]
   [mastodon-bot.twitter-domain :as twd]
   [mastodon-bot.tumblr-domain :as td]))

(s/def ::mastodon md/mastodon-auth?)
(s/def ::twitter twd/twitter-auth?)
(s/def ::tumblr td/tumblr-auth?)

(def auth? 
  (s/keys :opt-un [::mastodon ::twitter ::tumblr]))
(s/def ::auth auth?)

(s/def ::transform trd/transformations?)

(def config? 
  (s/keys :req-un [::auth ::transform]))

(s/def ::options (s/* #{"-h"}))
(s/def ::config-location (s/? (s/and string?
                                     #(not (cs/starts-with? % "-")))))
(s/def ::args (s/cat :options ::options 
                     :config-location ::config-location))
