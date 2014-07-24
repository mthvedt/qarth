(ns qarth.impl.yahoo
  "Implementation for Yahoo! The type is :yahoo."
  (require [qarth.oauth :as oauth]
           [qarth.oauth.lib :as lib]
           qarth.impl.scribe
           clojure.data.xml))

(qarth.impl.scribe/extend-scribe :yahoo :scribe-v1
                                 org.scribe.builder.api.YahooApi)

(defmethod oauth/id :yahoo
  [requestor]
  (-> {:url "https://social.yahooapis.com/v1/me/guid"}
    requestor :body clojure.data.xml/parse-str :content first :content first))
