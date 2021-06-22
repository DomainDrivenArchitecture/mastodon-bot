(ns mastodon-bot.transform
  (:require
   [orchestra.core :refer-macros [defn-spec]]
   [clojure.string :as string]
   [mastodon-bot.infra :as infra]
   [mastodon-bot.mastodon-domain :as md]
   [mastodon-bot.mastodon-api :as ma]
   [mastodon-bot.twitter-domain :as twd]
   [mastodon-bot.twitter-api :as twa]
   [mastodon-bot.rss-api :as ra]
   [mastodon-bot.tumblr-domain :as td]
   [mastodon-bot.tumblr-api :as ta]
   [mastodon-bot.transform-domain :as trd]))

(def shortened-url-pattern #"(https?://)?(?:\S+(?::\S*)?@)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,}))\.?)(?::\d{2,5})?(?:[/?#]\S*)?")

(defn-spec intermediate-resolve-urls trd/intermediate?
  [resolve-urls? ::trd/resolve-urls?
   input trd/intermediate?]
  (if resolve-urls?
    (update input :text #(string/replace % shortened-url-pattern infra/resolve-url))
    input))

(defn-spec content-filter-regexes ::trd/content-filters
  [transformation ::trd/transformation]
  (mapv re-pattern (:content-filters transformation)))

(defn-spec keyword-filter-regexes ::trd/keyword-filters
  [transformation ::trd/transformation]
  (mapv re-pattern (:keyword-filters transformation)))

(defn-spec blocked-content? boolean?
  [transformation ::trd/transformation
   text string?]
  (boolean
   (or (some #(re-find % text) (content-filter-regexes transformation))
       (when (not-empty (keyword-filter-regexes transformation))
         (empty? (some #(re-find % text) (keyword-filter-regexes transformation)))))))

(defn-spec perform-replacements trd/intermediate?
  [transformation ::trd/transformation
   input trd/intermediate?]
  (update input :text #(reduce-kv string/replace % (:replacements transformation))))

(defn-spec post-tweets-to-mastodon any?
  [mastodon-auth md/mastodon-auth?
   transformation ::trd/transformation
   last-post-time any?]
  (let [{:keys [source target resolve-urls?]} transformation]
    (fn [error tweets response]
      (if error
        (infra/exit-with-error error)
        (->> (infra/js->edn tweets)
             (map twa/parse-tweet)
             (filter #(> (:created-at %) last-post-time))
             (remove #(blocked-content? transformation (:text %)))
             (map #(intermediate-resolve-urls resolve-urls? %))
             (map #(twa/nitter-url source %))
             (map #(perform-replacements transformation %))
             (map #(ma/intermediate-to-mastodon target %))
             (ma/post-items mastodon-auth target))))))

(defn-spec tweets-to-mastodon any?
  [mastodon-auth md/mastodon-auth?
   twitter-auth twd/twitter-auth?
   transformation ::trd/transformation
   last-post-time any?]
  (let [{:keys [source target resolve-urls?]} transformation]
    (doseq [account (:accounts source)]
      (twa/user-timeline
       twitter-auth
       source
       account
       (post-tweets-to-mastodon 
        mastodon-auth
        transformation
        last-post-time)))))

(defn-spec post-tumblr-to-mastodon any?
  [mastodon-auth md/mastodon-auth?
   transformation ::trd/transformation
   last-post-time any?]
  (let [{:keys [source target resolve-urls?]} transformation]
    (fn [error tweets response]
      (if error
        (infra/exit-with-error error)
        (->> (infra/js->edn tweets)
             :posts
             (mapv ta/parse-tumblr-post)
             (filter #(> (:created-at %) last-post-time))             
             (remove #(blocked-content? transformation (:text %)))
             (map #(perform-replacements transformation %))
             (map #(ma/intermediate-to-mastodon target %))
             (ma/post-items mastodon-auth target))))))

(defn-spec tumblr-to-mastodon any?
  [mastodon-auth md/mastodon-auth?
   tumblr-auth td/tumblr-auth?
   transformation ::trd/transformation
   last-post-time any?]
  (let [{:keys [accounts limit]} transformation]
    (doseq [account accounts]
      (let [client (ta/tumblr-client tumblr-auth account)]
        (.posts client 
                #js {:limit (or limit 5)}
                (post-tumblr-to-mastodon
                 mastodon-auth
                 transformation
                 last-post-time)
                )))))

(defn-spec post-rss-to-mastodon any?
  [mastodon-auth md/mastodon-auth?
   transformation ::trd/transformation
   last-post-time any?]
  (let [{:keys [source target resolve-urls?]} transformation]
    (fn [payload]
      (->> (infra/js->edn payload)
           (:items)
           (map ra/parse-feed)
           (filter #(> (:created-at %) last-post-time))
           (remove #(blocked-content? transformation (:text %)))
           (map #(intermediate-resolve-urls resolve-urls? %))
           (map #(perform-replacements transformation %))
           (map #(ma/intermediate-to-mastodon target %))
           (ma/post-items mastodon-auth target)))))


(defn-spec rss-to-mastodon any?
  [mastodon-auth md/mastodon-auth?
   transformation ::trd/transformation
   last-post-time any?]
  (let [{:keys [source target]} transformation]
    (doseq [[name url] (:feeds source)]
      (ra/get-feed
       url
       (post-rss-to-mastodon
        mastodon-auth
        transformation
        last-post-time)))))