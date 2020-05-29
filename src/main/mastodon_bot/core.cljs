#!/usr/bin/env lumo

(ns mastodon-bot.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as st]
   [orchestra.core :refer-macros [defn-spec]]
   ["rss-parser" :as rss]
   [mastodon-bot.infra :as infra]
   [mastodon-bot.transform :as transform]
   [mastodon-bot.mastodon-api :as masto]
   [mastodon-bot.twitter-api :as twitter]
   [mastodon-bot.tumblr-api :as tumblr]
   [cljs.core :refer [*command-line-args*]]))

(s/def ::mastodon-auth masto/mastodon-auth?)
(s/def ::transform transform/transformations?)
(s/def ::twitter twitter/twitter-auth?)
(s/def ::tumblr map?)
(s/def ::rss map?)

(def config? (s/keys :req-un [::mastodon-config]
                     :opt-un [::twitter ::tumblr ::rss]))

(defn-spec mastodon-auth ::mastodon-auth
  [config config?]
  (:mastodon-config config))

(defn-spec twitter-auth ::twitter
  [config config?]
  (:twitter config))

(defn-spec transform ::transform
  [config config?]
  (:transform config))

(def config (infra/load-config))

(defn post-tumblrs [last-post-time]
  (fn [err response]
    (->> response
         infra/js->edn
         :posts
         (mapv tumblr/parse-tumblr-post)
         (map #(transform/intermediate-to-mastodon
                (mastodon-auth config)
                ;todo: fix this
                (:target (first (transform config))) %))
         (masto/post-items 
          (mastodon-auth config)
          (:target (first (transform config)))
          last-post-time))))

(defn parse-feed [last-post-time parser [title url]]
  (-> (.parseURL parser url)
      (.then #(masto/post-items
               (mastodon-auth config)
               (:target (first (transform config)))
               last-post-time
               (for [{:keys [title isoDate pubDate content link]} (-> % infra/js->edn :items)]
                 {:created-at (js/Date. (or isoDate pubDate))
                  :text (str (transform/trim-text
                              title
                              (masto/max-post-length (mastodon-auth config))) 
                             "\n\n" (twitter/strip-utm link))})))))

(defn -main []
  (let [mastodon-auth (mastodon-auth config)]
    (masto/get-mastodon-timeline
     mastodon-auth
     (fn [timeline]
       (let [last-post-time (-> timeline first :created_at (js/Date.))]
     ;;post from Twitter
         (when-let [twitter-auth (twitter-auth config)]
           (let [{:keys [transform]} config]
             (doseq [transformation transform]
               (transform/tweets-to-mastodon
                mastodon-auth
                twitter-auth
                transformation
                last-post-time))))
     ;;post from Tumblr
         (when-let [{:keys [access-keys accounts limit]} (:tumblr config)]
           (doseq [account accounts]
             (let [client (tumblr/tumblr-client access-keys account)]
               (.posts client #js {:limit (or limit 5)} (post-tumblrs last-post-time)))))
     ;;post from RSS
         (when-let [feeds (some-> config :rss)]
           (let [parser (rss.)]
             (doseq [feed feeds]
               (parse-feed last-post-time parser feed)))))))))

(set! *main-cli-fn* -main)
(st/instrument 'mastodon-auth)
(st/instrument 'twitter-auth)
(st/instrument 'transform)
