(ns qarth.impl.twitter
  "A Twitter oauth impl. Type is :twitter."
  (require (qarth [oauth :as oauth])
           [qarth.oauth.lib :as lib]
           qarth.impl.scribe
           cheshire.core))

(qarth.impl.scribe/extend-scribe :twitter :scribe-v1
                                 org.scribe.builder.api.TwitterApi)
(qarth.impl.scribe/extend-scribe :twitter-login :twitter
                                 org.scribe.builder.api.TwitterApi$Authenticate)

(defmethod oauth/id :twitter
  [requestor]
  (-> {:url "https://api.twitter.com/1.1/account/verify_credentials.json"}
    requestor :body cheshire.core/parse-string (get "id")))
