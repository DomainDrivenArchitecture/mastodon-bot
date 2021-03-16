(ns mastodon-bot.rss-domain-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.alpha :as s]
   [mastodon-bot.rss-domain :as sut]))

(deftest test-spec
  (is (s/valid? sut/rss-source?
                {:feeds [["correctiv-blog" "https://news.correctiv.org/news/rss.php"]]}
                )))
