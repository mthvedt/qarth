(ns qarth.impl.facebook
  "A Facebook oauth impl. Type is :facebook."
  (require (qarth [oauth :as oauth])
           qarth.impl.oauth-v2
           cheshire.core))

(oauth/derive :facebook :oauth)

(defmethod oauth/build :facebook
  [service]
  (assoc service
         :request-url "https://www.facebook.com/dialog/oauth"
         :access-url "https://graph.facebook.com/oauth/access_token"))

(defmethod oauth/id :facebook
  [requestor]
  (-> {:url "https://graph.facebook.com/me"}
    requestor :body cheshire.core/parse-string (get "id")))
