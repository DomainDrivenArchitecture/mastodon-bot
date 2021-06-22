(ns mastodon-bot.twitter-api
  (:require
   [clojure.spec.alpha :as s]
   [orchestra.core :refer-macros [defn-spec]]
   [clojure.string :as string]
   ["twitter" :as twitter]
   [mastodon-bot.infra :as infra]
   [mastodon-bot.twitter-domain :as td]
   ))

(defn-spec twitter-client any?
  [twitter-auth td/twitter-auth?]
  (try
    (twitter. (clj->js twitter-auth))
    (catch js/Error e
      (infra/exit-with-error
       (str "failed to connect to Twitter: " (.-message e))))))

(defn strip-utm [news-link]
  (first (string/split news-link #"\?utm")))

(defn in [needle haystack]
  (some (partial = needle) haystack))

; If the text ends in a link to the media (which is uploaded anyway),
; chop it off instead of including the link in the toot
(defn chop-tail-media-url [text media]
  (string/replace text #" (\S+)$" #(if (in (%1 1) (map :url media)) "" (%1 0))))

(defn parse-tweet [{created-at            :created_at
                    text                  :full_text
                    {:keys [media]}       :extended_entities
                    {:keys [screen_name]} :user :as tweet}]
  {:created-at (js/Date. created-at)
   :text (chop-tail-media-url text media)
   :screen_name screen_name
   :media-links (keep #(when (= (:type %) "photo") (:media_url_https %)) media)})

(defn-spec nitter-url map?
  [source td/twitter-source?
   parsed-tweet map?]
  (if (:nitter-urls? source)
    (update parsed-tweet :text #(string/replace % #"https://twitter.com" "https://nitter.net"))
    parsed-tweet))

(defn-spec user-timeline any?
  [twitter-auth td/twitter-auth?
   source td/twitter-source?
   account ::td/account]
  (let [{:keys [include-rts? include-replies?]} source]
    (infra/resolve-promise
     (.get (twitter-client twitter-auth)
           "statuses/user_timeline"
           #js {:screen_name account
                :tweet_mode "extended"
                :include_rts (boolean include-rts?)
                :exclude_replies (not (boolean include-replies?))})
     [])))
  