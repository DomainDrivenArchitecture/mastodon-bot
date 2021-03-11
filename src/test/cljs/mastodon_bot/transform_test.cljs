(ns mastodon-bot.transform-test
  (:require
   [cljs.test :refer-macros [deftest is testing run-tests]]
   [cljs.reader :as edn]
   ["fs" :as fs]
   [mastodon-bot.twitter-api :as twitter]
   [mastodon-bot.transform :as sut]
   ))

(defn readfile [filename]
  (-> filename (fs/readFileSync #js {:encoding "UTF-8"}) edn/read-string))

(def testconfig (readfile "test.edn"))

(deftest test-replacements
  (is (=
    "💠 Check out what has been going on during March in the world of @ReproBuilds! 💠 https://t.co/k6NsSO115z @opensuse@fosstodon.org @conservancy@mastodon.technology @PrototypeFund@mastodon.social @debian@fosstodon.org "
    (:text (sut/perform-replacements (first (:transform testconfig)) (twitter/parse-tweet (readfile "testdata/twitter/tweet-mentions.edn"))))
    )))