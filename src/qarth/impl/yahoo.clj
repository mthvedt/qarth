(ns qarth.impl.yahoo
  "Implementation for Yahoo! The type is :yahoo."
  (require [qarth.oauth :as oauth]
           [qarth.oauth.lib :as lib]
           qarth.impl.scribe
           clojure.data.xml))

(qarth.impl.scribe/extend-scribe :yahoo org.scribe.builder.api.YahooApi)

(defmethod oauth/extract-code :yahoo
  [service record request]
  (lib/do-extract-code (-> record :state first)
                       request :oauth_token :oauth_verifier :oauth_problem))

(defmethod oauth/id :yahoo
  [requestor]
  (-> {:url "https://social.yahooapis.com/v1/me/guid"}
    requestor :body clojure.data.xml/parse-str :content first :content first))
