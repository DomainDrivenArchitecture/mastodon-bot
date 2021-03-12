(ns mastodon-bot.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as st]
   [orchestra.core :refer-macros [defn-spec]]
   [expound.alpha :as expound]
   [mastodon-bot.infra :as infra]
   [mastodon-bot.core-domain :as cd]
   [mastodon-bot.transform :as transform]
   [mastodon-bot.mastodon-api :as ma]))

(set! s/*explain-out* expound/printer)

(defn-spec mastodon-auth ::cd/mastodon
  [config cd/config?]
  (get-in config [:auth :mastodon]))

(defn-spec twitter-auth ::cd/twitter
  [config cd/config?]
  (get-in config [:auth :twitter]))

(defn-spec tumblr-auth ::cd/tumblr
  [config cd/config?]
  (get-in config [:auth :tumblr]))

(defn-spec transform ::cd/transform
  [config cd/config?]
  (:transform config))

(defn-spec transform! any?
  [config cd/config?]
  (let [mastodon-auth (mastodon-auth config)]  
    (ma/get-mastodon-timeline
     mastodon-auth
     (fn [timeline]
       (let [last-post-time (-> timeline first :created_at (js/Date.))]
         (let [{:keys [transform]} config]
           (doseq [transformation transform]
             (let [source-type (get-in transformation [:source :source-type])
                   target-type (get-in transformation [:target :target-type])]               
               (cond
               ;;post from Twitter
                 (and (= :twitter source-type)
                      (= :mastodon target-type))
                 (when-let [twitter-auth (twitter-auth config)]
                   (transform/tweets-to-mastodon
                    mastodon-auth
                    twitter-auth
                    transformation
                    last-post-time))
               ;;post from RSS
                 (and (= :rss source-type)
                      (= :mastodon target-type))
                 (transform/rss-to-mastodon
                  mastodon-auth
                  transformation
                  last-post-time)
               ;;post from Tumblr
                 (and (= :tumblr source-type)
                      (= :mastodon target-type))
                 (when-let [tumblr-auth (tumblr-auth config)]
                   (transform/tumblr-to-mastodon
                    mastodon-auth
                    tumblr-auth
                    transformation
                    last-post-time))
                 ))))
)))))

(def usage
  "usage:
  
  mastodon-bot /path/to/config.edn
  
  set MASTODON_BOT_CONFIG environment variable to run without parameters
  set MASTODON_BOT_CREDENTIALS environment variable to find :auth part in a separate file. Both configs will be merged.
  ")

(defn main [& args]
  (let [parsed-args (s/conform ::cd/args args)]
    (if (= ::s/invalid parsed-args)
      (do (s/explain ::cd/args args)
          (infra/exit-with-error (str "Bad commandline arguments\n" usage)))
      (let [{:keys [options config-location]} parsed-args]
        (cond
          (some #(= "-h" %) options)
          (print usage)
          :default
          (let [config (infra/load-config config-location)]
            (when (not (s/valid? cd/config? config))
              (s/explain cd/config? config)
              (infra/exit-with-error "Bad configuration"))
            (transform! config)))))))

(st/instrument 'mastodon-auth)
(st/instrument 'twitter-auth)
(st/instrument 'transform)
