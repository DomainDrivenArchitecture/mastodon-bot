(ns mastodon-bot.tumblr-api
  (:require
   [orchestra.core :refer-macros [defn-spec]]
   [clojure.string :as string]
   [mastodon-bot.tumblr-domain :as td]
   [mastodon-bot.infra :as infra]
   ["tumblr" :as tumblr]
   ))

(defn-spec tumblr-client any?
  [access-keys td/tumblr-auth?
   account string?]
  (try
    (tumblr/Blog. account (clj->js access-keys))
    (catch js/Error e
      (infra/exit-with-error
       (str "failed to connect to Tumblr account " account ": " (.-message e))))))

(defmulti parse-tumblr-post :type)

(defmethod parse-tumblr-post "text" [{:keys [body date short_url]}]
  {:created-at (js/Date. date)
   :text body
   :untrimmed-text (str "\n\n" short_url)})

(defmethod parse-tumblr-post "photo" [{:keys [caption date photos short_url] :as post}]
  {:created-at (js/Date. date)
   :text (string/join "\n" [(string/replace caption #"<[^>]*>" "") short_url])
   :media-links (mapv #(-> % :original_size :url) photos)})

(defmethod parse-tumblr-post :default [post])