(ns mastodon-bot.mastodon-api
  (:require
   [orchestra.core :refer-macros [defn-spec]]
   [clojure.string :as string]
   [mastodon-bot.mastodon-domain :as m]
   [mastodon-bot.infra :as infra]
   ["request" :as request]
   ["mastodon-api" :as mastodon]))

(def mastodon-target-defaults {:append-screen-name? false
                               :visibility "public"
                               :sensitive? true
                               :media-only? false
                               :max-post-length 300})

(defn trim-text [text max-post-length]
  (cond

    (nil? max-post-length)
    text

    (> (count text) max-post-length)
    (reduce
     (fn [text word]
       (if (> (+ (count text) (count word)) (- max-post-length 3))
         (reduced (str text "..."))
         (str text " " word)))
     ""
     (string/split text #" "))

    :else text))

(defn-spec max-post-length ::m/max-post-length
  [target m/mastodon-target?]
  (:max-post-length target))

(defn-spec mastodon-client any?
  [mastodon-auth m/mastodon-auth?]
  (or (some-> mastodon-auth 
       clj->js 
       mastodon.)
      (infra/exit-with-error "missing Mastodon auth configuration!")))

(defn-spec delete-status any?
  [mastodon-auth m/mastodon-auth?
   status-id string?]
  (.delete (mastodon-client mastodon-auth) (str "statuses/" status-id) #js {}))

(defn post-status
  ([mastodon-auth target status-text]
   (post-status mastodon-auth target status-text nil print))
  ([mastodon-auth target status-text media-ids]
   (post-status mastodon-auth target status-text media-ids print))
  ([mastodon-auth target status-text media-ids callback]
   (let [{:keys [visibility sensitive?]} target]
     (-> (.post (mastodon-client mastodon-auth) "statuses"
                (clj->js (merge {:status status-text}
                                (when media-ids {:media_ids media-ids})
                                (when sensitive? {:sensitive sensitive?})
                                (when visibility {:visibility visibility}))))
         (.then #(-> % callback))))))

(defn-spec post-image any?
  [mastodon-auth m/mastodon-auth?
   target m/mastodon-target?
   image-stream any?
   description string?
   callback fn?]
  (-> (.post (mastodon-client mastodon-auth) "media" 
             #js {:file image-stream :description description})
      (.then #(-> % .-data .-id callback))))

(defn post-status-with-images
  ([mastodon-auth target status-text urls]
   (post-status-with-images mastodon-auth target status-text urls [] print))
  ([mastodon-auth target status-text urls ids]
   (post-status-with-images mastodon-auth target status-text urls ids print))
  ([mastodon-auth target status-text [url & urls] ids callback]
   (if url
     (-> request
         (.get url)
         (.on "response"
           (fn [image-stream]
             (post-image mastodon-auth target image-stream status-text 
                         #(post-status-with-images mastodon-auth 
                                                   target
                                                   status-text 
                                                   urls 
                                                   (conj ids %) 
                                                   callback)))))
     (post-status mastodon-auth target status-text (not-empty ids) callback))))

(defn-spec post-items any?
  [mastodon-auth m/mastodon-auth?
   target m/mastodon-target?
   items any?]
  (doseq [{:keys [text media-links]} items]
    (if media-links
      (post-status-with-images mastodon-auth target text media-links)
      (when-not (:media-only? target)
        (post-status mastodon-auth target text)))))

(defn-spec get-mastodon-timeline any?
  [mastodon-auth m/mastodon-auth?
   callback fn?]
  (.then (.get (mastodon-client mastodon-auth)
               (str "accounts/" (:account-id mastodon-auth) "/statuses") #js {})
         #(let [response (-> % .-data infra/js->edn)]
            (if-let [error (::error response)]
              (infra/exit-with-error error)
              (callback response)))))

(defn-spec intermediate-to-mastodon m/mastodon-output?
  [target m/mastodon-target?
   input any?]
  (let [target-with-defaults (merge mastodon-target-defaults
                                    target)
        {:keys [created-at text media-links screen_name untrimmed-text]} input
        {:keys [signature append-screen-name?]} target-with-defaults
        untrimmed (if (some? untrimmed-text)
                    (str " " untrimmed-text) "")
        sname (if append-screen-name?
                (str "\n#" screen_name) "")
        signature_text (if (some? signature)
                         (str "\n" signature)
                         "")
        trim-length (- (max-post-length target-with-defaults)
                       (count untrimmed)
                       (count sname)
                       (count signature_text))]
    {:created-at created-at
     :text (str (trim-text text trim-length)
                untrimmed
                sname
                signature_text)
     :reblogged true
     :media-links media-links}))