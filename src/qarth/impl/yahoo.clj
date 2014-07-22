(ns qarth.impl.yahoo
  "Implementation for Yahoo! The type is :yahoo.com."
  (require [qarth.oauth :as oauth]
           [qarth.oauth.lib :as lib]
           qarth.impl.scribe
           clojure.data.xml))

(qarth.impl.scribe/extend-scribe :yahoo.com org.scribe.builder.api.YahooApi)

(defmethod oauth/extract-code :yahoo.com
  [service record request]
  (lib/do-extract-code (-> record :state first)
                       request :oauth_token :oauth_verifier :oauth_problem))

(defmethod oauth/id :yahoo.com
  [requestor]
  (with-open [body (requestor {:url "https://social.yahooapis.com/v1/me/guid"})]
    (-> body clojure.data.xml/parse :content first :content first)))
