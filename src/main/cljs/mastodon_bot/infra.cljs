(ns mastodon-bot.infra
  (:require
   [cljs.reader :as edn]
   [clojure.string :as string]
   ["fs" :as fs]
   ["deasync" :as deasync]
   ["node-fetch" :as fetch]))

(defn log-error [item]
  (js/console.error item)
  item)

(defn log [item]
  (js/console.log item)
  item)

(defn log-first [item]
  (js/console.log (first item))
  item)

(defn js->edn [data]
  (js->clj data :keywordize-keys true))

(defn exit-with-error [error]
  (js/console.error error)
  (js/process.exit 1))

(defn find-config [config-location]
  (or config-location
      (-> js/process .-env .-MASTODON_BOT_CONFIG)
      "config.edn"))

(defn read-edn [config]
  (if config
    (if (fs/existsSync config)
       ;(edn/read-string (fs/readFileSync #js {:encoding "UTF-8"} config))
      (edn/read-string (fs/readFileSync config "UTF-8"))
      (exit-with-error (str "config file does not exist: " config)))
    nil))

(defn load-credentials-config []
  (read-edn (-> js/process .-env .-MASTODON_BOT_CREDENTIALS)))

(defn load-main-config [config-location]
  (-> config-location
      (find-config)
      (read-edn)))

(defn load-config [config-location]
  (merge (load-main-config config-location) (load-credentials-config)))

(defn resolve-promise [promise result-on-error]
  (let [done (atom false)
        result (atom nil)]
    (-> promise
        (.then #(do (reset! result %) (reset! done true)))
        (.catch #(do
                   (log-error %)
                   (reset! result result-on-error)
                   (reset! done true))))
    (.loopWhile deasync (fn [] (not @done)))
    @result))

(defn resolve-url [[uri]]
  (let [used-uri (if (string/starts-with? uri "https://") uri (str "https://" uri))
        location (-> (fetch used-uri #js {:method "GET" :redirect "manual" :timeout "3000"})
                     (.then #(.get (.-headers %) "Location"))
                     (.then #(string/replace % "?mbid=social_twitter" "")))]
    (resolve-promise location uri)))
