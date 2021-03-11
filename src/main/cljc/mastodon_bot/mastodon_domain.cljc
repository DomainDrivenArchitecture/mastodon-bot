(ns mastodon-bot.mastodon-domain
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::access_token string?)
(s/def ::api_url string?)
(s/def ::account-id string?)
(s/def ::append-screen-name? boolean?)
(s/def ::signature string?)
(s/def ::sensitive? boolean?)
(s/def ::media-only? boolean?)
(s/def ::visibility #{"direct" "private" "unlisted" "public"})
(s/def ::max-post-length (fn [n] (and
                                  (int? n)
                                  (<= n 500)
                                  (> n 0))))
(def mastodon-auth? (s/keys :req-un [::account-id ::access_token ::api_url]))
(def mastodon-target? (s/keys :opt-un [::max-post-length
                                       ::signature
                                       ::visibility
                                       ::append-screen-name?
                                       ::sensitive?
                                       ::media-only?]))
(s/def ::created-at any?)
(s/def ::text string?)
(s/def ::media-links string?)

(def mastodon-output?  (s/keys :req-un [::created-at ::text]
                               :opt-un [::media-links]))
