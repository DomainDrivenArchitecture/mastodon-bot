(ns mastodon-bot.transform-domain-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.alpha :as s]
   [mastodon-bot.transform-domain :as sut]
   ))

(deftest test-spec
  (is (s/valid? sut/transformations?
                []))
  (is (s/valid? sut/transformations?
                [{:source {:source-type :twitter
                           :include-replies? false
                           :include-rts? true
                           :nitter-urls? true
                           :accounts ["an-twitter-account"]}
                  :target {:target-type :mastodon
                           :append-screen-name? true
                           :media-only? false
                           :max-post-length 500
                           :visibility "unlisted"
                           :sensitive? true
                           :signature "my-bot"}
                  :resolve-urls? true
                  :content-filters [".*bannedsite.*"]
                  :keyword-filters [".*"]}])))
