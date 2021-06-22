(ns mastodon-bot.infra-test
  (:require
   [cljs.test :refer-macros [deftest is testing run-tests]]
   [mastodon-bot.infra :as sut]))

;TODO: mbid test
(deftest test-resolve-uri
  (is (= "https://www.meissa-gmbh.de"
         (sut/resolve-url ["https://www.meissa-gmbh.de"])))
  (is (= "https://www.doesnotexist-blablabla.de"
         (sut/resolve-url ["https://www.doesnotexist-blablabla.de"])))
  (is (= "http://www.google.de/"
         (sut/resolve-url ["https://t1p.de/44oo"]))))