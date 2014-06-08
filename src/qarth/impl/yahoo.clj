(ns qarth.impl.yahoo
  "Implementation for Yahoo! The type is :yahoo.com."
  (require [qarth.oauth :as oauth]
           [qarth.oauth.lib :as lib]
           qarth.impl.scribe
           clojure.xml))

(qarth.impl.scribe/extend-scribe :yahoo.com org.scribe.builder.api.YahooApi)

(defmethod oauth/extract-verifier :yahoo.com
  [service record request]
  (lib/do-extract-verifier (-> record :request-token first)
                           request :oauth-token :oauth-verifier :oauth_problem))

(defmethod oauth/id :yahoo.com
  [requestor]
  (-> (requestor {:url "https://social.yahooapis.com/v1/me/guid"})
    :body clojure.xml/parse :content first :content first))
